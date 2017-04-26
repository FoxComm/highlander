package models.activity

import com.github.tminglei.slickpg.LTree
import com.typesafe.scalalogging.LazyLogging
import io.circe.jackson.syntax._
import io.circe.syntax._
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Properties
import models.account.Scope
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import scala.concurrent.Future
import scala.util.{Failure, Success}
import utils.Environment
import utils.FoxConfig.config
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.json._
import utils.json.codecs._

case class ActivityContext(userId: Int, userType: String, transactionId: String, scope: LTree) {
  def withCurrentScope(implicit au: AU): ActivityContext = withScope(Scope.current)
  def withScope(scope: LTree): ActivityContext =
    ActivityContext(userId, userType, transactionId, scope)
}

object ActivityContext {

  // Convert context to json and back again
  implicit val ActivityContextColumn: BaseColumnType[ActivityContext] =
    dbJsonColumn[ActivityContext]

  def build(userId: Int, userType: String, scope: LTree, transactionId: String): ActivityContext =
    ActivityContext(userId = userId,
                    userType = userType,
                    transactionId = transactionId,
                    scope = scope)
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

  val producer =
    if (Environment.default != Environment.Test)
      Some(new KafkaProducer[GenericData.Record, GenericData.Record](kafkaProducerProps()))
    else None

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

  def kafkaProducerProps(): Properties = {
    val props = new Properties()

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.apis.kafka.bootStrapServersConfig)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.apis.kafka.keySerializer)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.apis.kafka.valueSerializer)
    props.put("schema.registry.url", config.apis.kafka.schemaRegistryURL)
    props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, config.apis.kafka.producerTimeout)

    props
  }

  def log(a: OpaqueActivity)(implicit activityContext: AC, ec: EC): DbResultT[Activity] = {
    val activity = Activity(id = "",
                            activityType = a.activityType,
                            data = a.data,
                            context = activityContext,
                            createdAt = Instant.now)

    val record = new GenericData.Record(schema)

    record.put("kind", activity.activityType)
    record.put("data", activity.data.jacksonPrint)
    record.put("context", activity.context.asJson.jacksonPrint)
    record.put("created_at", DateTimeFormatter.ISO_INSTANT.format(activity.createdAt))
    record.put("scope", activity.context.scope.toString())

    val key = new GenericData.Record(keySchema)
    key.put("id", activity.id.toString)

    for {
      id ← * <~ nextActivityId()
      phoenixId = s"phoenix-$id"
      _         = record.put("id", phoenixId)
      _         = sendActivity(activity, record)
    } yield activity.copy(id = phoenixId)
  }

  def nextActivityId()(implicit ec: EC): DbResultT[Int] =
    sql"select nextval('activities_id_seq');".as[Int].head.dbresult

  def sendActivity(a: Activity, record: GenericData.Record)(implicit activityContext: AC, ec: EC) {
    val msg = new ProducerRecord[GenericData.Record, GenericData.Record](topic, record)

    // Workaround until we decide how to test Phoenix => Kafka service integration
    producer match {

      case Some(p) ⇒ {
        val kafkaSendFuture = Future {
          p.send(msg)
        }

        kafkaSendFuture onComplete {
          case Success(_) ⇒
            logger.info(
                s"Kafka Activity ${a.activityType} by ${activityContext.userType} ${activityContext.userId} SUCCESS")
          case Failure(_) ⇒
            logger.info(
                s"Kafka Activity ${a.activityType} by ${activityContext.userType} ${activityContext.userId} FAILURE")
        }
      }
      case None ⇒
        logger.info(
            s"Test Kafka Activity ${a.activityType} by ${activityContext.userType} ${activityContext.userId}")
    }
  }
}
