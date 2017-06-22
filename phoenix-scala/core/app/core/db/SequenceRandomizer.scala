package core.db

import scala.util.Random
import slick.jdbc.PostgresProfile.api._

object SequenceRandomizer {

  // When changing this, please, if anything, make them less predictable, not more. @michalrus
  def randomizeSchema(schema: String)(implicit ec: EC): DBIO[Unit] =
    for {
      allSequences ← sql"SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = $schema"
                      .as[String]

      // TODO: Make it possible to not filter these out… @michalrus
      randomizedSequences = allSequences.filterNot(
        Set(
          "scopes_id_seq", // FIXME: What the hell. https://foxcommerce.slack.com/archives/C06696D1R/p1495796779988723
          "object_contexts_id_seq" // FIXME: Sigh. https://foxcommerce.slack.com/archives/C06696D1R/p1495798791447479
        ) contains _)

      gap = 1000000
      withValues = Random
        .shuffle(randomizedSequences)
        .zip(Stream.from(1).map(_ * gap + Random.nextInt(gap / 10)))

      _ ← DBIO
           .sequence(withValues.map {
             case (name, value) ⇒
               val increment     = (if (Random.nextBoolean()) 1 else -1) * Random.nextInt(100)
               val incrementNon0 = if (increment == 0) -1 else increment
               sql"ALTER SEQUENCE #$name START WITH #$value INCREMENT BY #$incrementNon0 RESTART".asUpdate
           })

    } yield ()

}
