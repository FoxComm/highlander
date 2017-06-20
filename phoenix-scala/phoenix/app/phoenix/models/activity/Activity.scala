package phoenix.models.activity

import com.github.tminglei.slickpg.LTree
import com.typesafe.scalalogging.LazyLogging
import core.db.ExPostgresDriver.api._
import core.db._
import java.time.Instant
import java.time.format.DateTimeFormatter
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.{Callback, Producer, ProducerRecord, RecordMetadata}
import org.json4s.Extraction
import org.json4s.jackson.Serialization.{write ⇒ render}
import phoenix.models.account.Scope
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import scala.concurrent.Future
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

case class ActivityContext(userId: Int, userType: String, transactionId: String, scope: LTree) {
  def withCurrentScope(implicit au: AU): ActivityContext = withScope(Scope.current)
  def withScope(scope: LTree): ActivityContext =
    ActivityContext(userId, userType, transactionId, scope)
}

object ActivityContext {

  // Convert context to json and back again
  implicit val ActivityContextColumn: JdbcType[ActivityContext] with BaseTypedType[ActivityContext] = {
    implicit val formats = JsonFormatters.phoenixFormats
    MappedColumnType.base[ActivityContext, Json](
      c ⇒ Extraction.decompose(c),
      j ⇒ j.extract[ActivityContext]
    )
  }
}

case class EnrichedActivityContext(ctx: ActivityContext,
                                   producer: Producer[GenericData.Record, GenericData.Record])

object EnrichedActivityContext {
  implicit def enrichActivityContext(implicit ctx: ActivityContext, apis: Apis): AC =
    EnrichedActivityContext(ctx, apis.kafka)
}

/**
  * An activity keeps information about some interesting change in state. The data an
  * activity contains must be complete enough to render in the UI. The activity also
  * keeps track of what/who created it.
  *
  * An activity can be part of many activity trails in multiple dimensions.
  */
case class Activity(id: String,
                    activityType: ActivityType,
                    data: Json,
                    context: ActivityContext,
                    createdAt: Instant = Instant.now)

// Any specific activity can have an implicit converion function to the opaque activity
// Opaque here means the scala type system cannot see the activity
case class OpaqueActivity(activityType: ActivityType, data: Json)

object Activities extends LazyLogging {
  val topic  = "scoped_activities"
  val schema = new Schema.Parser().parse("""
      |{
      |  "type":"record",
      |  "name":"scoped_activities",
      |  "fields":[
      |    {
      |      "name":"id",
      |      "type":["null","string"]
      |    },
      |    {
      |      "name":"kind",
      |      "type":["null","string"]
      |    },
      |    {
      |      "name":"data",
      |      "type":["null","string"]
      |    },
      |    {
      |      "name":"context",
      |      "type":["null","string"]
      |    },
      |    {
      |      "name":"created_at",
      |      "type":["null","string"]
      |    },
      |    {
      |      "name":"scope",
      |      "type":["null","string"]
      |    }
      |  ]
      |}
    """.stripMargin.replaceAll("\n", " "))

  val keySchema = new Schema.Parser().parse("""
      |{
      |  "type":"record",
      |  "name":"scoped_activities_key",
      |  "fields":[
      |    {
      |      "name":"id",
      |      "type":["null","string"]
      |    }
      |  ]
      |}
    """.stripMargin.replaceAll("\n", " "))

  implicit val formats = JsonFormatters.phoenixFormats

  def log(a: OpaqueActivity)(implicit activityContext: AC, ec: EC): DbResultT[Activity] = {
    val activity = Activity(id = "",
                            activityType = a.activityType,
                            data = a.data,
                            context = activityContext.ctx,
                            createdAt = Instant.now)

    val record = new GenericData.Record(schema)

    record.put("kind", activity.activityType)
    record.put("data", render(activity.data))
    record.put("context", render(activity.context))
    record.put("created_at", DateTimeFormatter.ISO_INSTANT.format(activity.createdAt))
    record.put("scope", activity.context.scope.toString())

    val key = new GenericData.Record(keySchema)

    for {
      id ← * <~ nextActivityId()
      phoenixId = s"phoenix-$id"
      _         = record.put("id", phoenixId)
      _         = key.put("id", phoenixId)
      _         = sendActivity(activity, key, record)
    } yield activity.copy(id = phoenixId)
  }

  def nextActivityId()(implicit ec: EC): DbResultT[Int] =
    sql"select nextval('activities_id_seq');".as[Int].head.dbresult

  private def sendActivity(a: Activity, key: GenericData.Record, record: GenericData.Record)(
      implicit activityContext: AC,
      ec: EC): Unit = {
    val msg = new ProducerRecord[GenericData.Record, GenericData.Record](topic, key, record)

    activityContext.producer.send(
      msg,
      new Callback {
        // we force logging to be done in `ec` execution context
        // instead of a kafka background thread that executes callbacks
        def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = Future {
          if (metadata ne null)
            logger.info(
              s"Kafka Activity ${a.activityType} by ${activityContext.ctx.userType} ${activityContext.ctx.userId} SUCCESS")
          else
            logger.error(
              s"Kafka Activity ${a.activityType} by ${activityContext.ctx.userType} ${activityContext.ctx.userId} FAILURE",
              exception)
        }
      }
    )
  }
}
