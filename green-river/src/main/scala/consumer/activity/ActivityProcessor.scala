package consumer.activity

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Properties

import scala.concurrent.Future
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io._
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

import consumer.{AvroJsonHelper, JsonProcessor, AvroProcessor}
import consumer.aliases._
import consumer.failures.{Failures, GeneralFailure}
import consumer.utils.HttpSupport.HttpResult
import consumer.utils.{Phoenix, PhoenixConnectionInfo}
import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.{JNothing, JValue}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.{write ⇒ render}
import cats.implicits._

final case class ActivityContext(
    userId: Int, userType: String, transactionId: String, scope: String)

final case class Activity(id: String,
                          kind: String,
                          data: JValue,
                          context: ActivityContext,
                          createdAt: String)

final case class Connection(dimension: String, objectId: String, data: JValue, activityId: String)

final case class AppendNotification(
    sourceDimension: String, sourceObjectId: String, activity: Activity)

trait ActivityConnector {
  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]]
}

final case class FailedToConnectActivity(
    activityId: String, dimension: String, objectId: String, failures: Failures)
    extends RuntimeException(
        s"Failed to connect activity $activityId to dimension '$dimension' and object $objectId " +
        s"failures: $failures")

final case class FailedToConnectNotification(
    activityId: String, dimension: String, objectId: String, response: HttpResponse)
    extends RuntimeException(
        s"Failed to create notification for connection of activity $activityId to dimension " +
        s"'$dimension' and object $objectId response: $response")

final case class KafkaConnectionInfo(broker: String, schemaRegistryURL: String)

//TODO: Convert to a JsonTransformer so we can use in scoped indexer
/**
  * This is a JsonProcessor which listens to the activity stream and processes the activity
  * using a sequence of activity connectors
  */
class ActivityProcessor(
    kafka: KafkaConnectionInfo, conn: PhoenixConnectionInfo, connectors: Seq[ActivityConnector])(
    implicit ec: EC, ac: AS, mat: AM, cp: CP, sc: SC)
    extends JsonProcessor {

  implicit val formats: DefaultFormats.type = DefaultFormats

  val activityJsonFields = List("id", "activityType", "data", "context", "createdAt")
  val phoenix            = Phoenix(conn)
  val kafkaProps = {
    val props = new Properties()

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.broker)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
              "io.confluent.kafka.serializers.KafkaAvroSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
              "io.confluent.kafka.serializers.KafkaAvroSerializer")
    props.put("schema.registry.url", kafka.schemaRegistryURL)

    props
  }

  val kafkaProducer = new KafkaProducer[GenericData.Record, GenericData.Record](kafkaProps)
  val trailTopic    = "scoped_activity_trails"

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {

    val activityJson = AvroJsonHelper.transformJson(inputJson, activityJsonFields)
    val activity     = parse(activityJson).extract[Activity]

    Console.err.println(
        s"Got Activity ${activity.kind} with ID ${activity.id} created at ${activity.createdAt}")
    if (activity.context == null) {
      Console.err.println(
          s"Warning, got Activity ${activity.kind} with ID ${activity.id} without a context, skipping...")
      Future { () }
    } else {
      val result = connectors.map { connector ⇒
        for {
          connections ← connector.process(offset, activity)
          responses   ← process(activity, connections)
        } yield responses
      }

      val responses = Future.sequence(result).map(_.flatten)

      //TODO check errors
      responses.map { r ⇒
        if (r.length == 0)
          System.err.println(s"Warning, MISSING CONNECTOR: ${activity.kind}")
        ()
      }
    }
  }

  private def process(activity: Activity, cs: Seq[Connection]): Future[Seq[Unit]] = {
    Future.sequence(cs.map(c ⇒ pushActivityConnectionToKafka(activity, c)) ++ cs.map(c ⇒
              createPhoenixNotification(activity, c, phoenix)))
  }

  private def pushActivityConnectionToKafka(activity: Activity, connection: Connection) = Future {
    val record = new GenericData.Record(AvroProcessor.activityTrailSchema)

    //TODO: Integer here is a problem. We need to modify mappings to do long for id for all indices.
    val id = java.util.UUID.randomUUID().getMostSignificantBits() & Integer.MAX_VALUE

    record.put("id", id)
    record.put("dimension", connection.dimension)
    record.put("object_id", connection.objectId)
    record.put("activity", render(activity))
    record.put("created_at", activity.createdAt)
    record.put("scope", activity.context.scope)

    val key = new GenericData.Record(AvroProcessor.keySchema)
    key.put("id", activity.id)

    kafkaProducer.send(
        new ProducerRecord[GenericData.Record, GenericData.Record](trailTopic, key, record))
    ()
  }

  private def createPhoenixNotification(
      activity: Activity, conn: Connection, phoenix: Phoenix): Future[Unit] = Future {
    val body = AppendNotification(sourceDimension = conn.dimension,
                                  sourceObjectId = conn.objectId,
                                  activity = activity)

    val notification = render(body)
    Console.err.println(s"POST /notifications, $notification")

    phoenix
      .post("notifications", notification)
      .fold({ failures ⇒
        Console.err.println(s"Failed Notification ${conn.dimension} ${conn.activityId}: $failures")
        throw FailedToConnectActivity(conn.activityId, conn.dimension, conn.objectId, failures)
      }, { resp ⇒
        if (resp.status != StatusCodes.OK) {
          Console.err.println(s"Failed Notification ${conn.dimension} ${conn.activityId}: $resp")
          throw new FailedToConnectNotification(
              conn.activityId, conn.dimension, conn.objectId, resp)
        }
        resp
      })
      .map(_ ⇒ ())
  }
}
