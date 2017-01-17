package time

import java.time.{Instant, ZoneId, ZonedDateTime}

import testutils._
import utils.db._

class JavaTimeMapperTest extends IntegrationTestBase with DbTestSupport {

  import api._

  "java.time mapper" - {
    class Cards(tag: Tag) extends Table[(Long, Instant)](tag, "java_time_test") {
      def id        = column[Long]("id", O.PrimaryKey)
      def deletedAt = column[Instant]("deleted_at")
      def *         = (id, deletedAt)
    }

    val query = TableQuery[Cards]
    val ddl =
      sqlu"create table java_time_test (id bigint primary key, deleted_at timestamp without time zone)"

    "writes instants correctly" ignore {
      val originalInstant: Instant = Instant.now()

      val (_, timestampAfterRoundtrip) = db
        .run((for {
          _    ← ddl
          _    ← query += ((1, originalInstant))
          read ← query.filter(_.id === 1L).result.head
          _    ← query.schema.drop
        } yield read).transactionally)
        .futureValue

      timestampAfterRoundtrip must === (originalInstant)
    }

    "reads instants correctly" in {
      val (_, timestampAfterRoundtrip) = db
        .run((for {
          _    ← ddl
          _    ← sqlu"""insert into java_time_test (id, deleted_at) values (1, '2015-07-01 15:17:38.0Z' at time zone 'utc')"""
          read ← query.filter(_.id === 1L).result.head
          _    ← query.schema.drop
        } yield read).withPinnedSession)
        .futureValue

      timestampAfterRoundtrip must === (
        ZonedDateTime.of(2015, 7, 1, 15, 17, 38, 0, ZoneId.of("UTC")).toInstant)
    }
  }
}
