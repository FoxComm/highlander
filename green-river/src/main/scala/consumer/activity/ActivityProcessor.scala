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
import org.json4s.JsonAST.{JNothing, JValue}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.{write ⇒ render}
import cats.implicits._

final case class ActivityContext(userId: Int, userType: String, transactionId: String)

final case class Activity(id: Int = 0,
                          activityType: String,
                          data: JValue,
                          context: ActivityContext,
                          createdAt: Instant = Instant.now,
                          scope: String)

final case class Connection(dimension: String, objectId: String, data: JValue, activityId: Int)

final case class AppendActivity(activityId: Int, data: JValue)
final case class AppendNotification(
    sourceDimension: String, sourceObjectId: String, activityId: Int, data: JValue)

trait ActivityConnector {
  def process(offset: Long, activity: Activity)(implicit ec: EC): Future[Seq[Connection]]
}

final case class FailedToConnectActivity(
    activityId: Int, dimension: String, objectId: String, failures: Failures)
    extends RuntimeException(
        s"Failed to connect activity $activityId to dimension '$dimension' and object $objectId " +
        s"failures: $failures")

final case class FailedToConnectNotification(
    activityId: Int, dimension: String, objectId: String, response: HttpResponse)
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
              "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
              "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("schema.registry.url", kafka.schemaRegistryURL)

    props
  }

  val kafkaProducer = new KafkaProducer[Array[Byte], Array[Byte]](kafkaProps)
  val trailTopic    = "scoped_activity_trails"

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {

    val activityJson = AvroJsonHelper.transformJson(inputJson, activityJsonFields)
    val activity     = parse(activityJson).extract[Activity]

    Console.err.println()
    Console.err.println(s"Got Activity ${activity.activityType} with ID ${activity.id}")
    if (activity.context == null) {
      Console.err.println(
          s"Warning, got Activity ${activity.activityType} with ID ${activity.id} without a context, skipping...")
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
          System.err.println(s"Warning, MISSING CONNECTOR: ${activity.activityType}")
        ()
      }
    }
  }

  private def process(activity: Activity, cs: Seq[Connection]): Future[Seq[Unit]] = {
    Future.sequence(cs.map(c ⇒ pushActivityConnectionToKafka(activity, c)))
  }

  private def pushActivityConnectionToKafka(activity: Activity, connection: Connection) = Future {
    val record = new GenericData.Record(AvroProcessor.activityTrailSchema)

    record.put("id", activity.id)
    record.put("dimension", connection.dimension)
    record.put("object_id", connection.objectId)
    record.put("activity", render(activity))
    record.put("created_at", DateTimeFormatter.ISO_INSTANT.format(activity.createdAt))
    record.put("scope", activity.scope)

    val writer                 = new SpecificDatumWriter[GenericRecord](AvroProcessor.activityTrailSchema)
    val out                    = new ByteArrayOutputStream()
    val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(out, null)
    writer.write(record, encoder)
    encoder.flush()
    out.close()
    val bytes: Array[Byte] = out.toByteArray()
    kafkaProducer.send(new ProducerRecord[Array[Byte], Array[Byte]](trailTopic, bytes))
    ()
  }

  private def createPhoenixNotification(conn: Connection, phoenix: Phoenix): HttpResult = {
    val body = AppendNotification(sourceDimension = conn.dimension,
                                  sourceObjectId = conn.objectId,
                                  activityId = conn.activityId,
                                  data = JNothing)

    val notification = render(body)
    Console.err.println(s"POST /notifications, $notification")

    phoenix.post("notifications", notification).map { response ⇒
      if (response.status != StatusCodes.OK) {
        throw new FailedToConnectNotification(
            conn.activityId, conn.dimension, conn.objectId, response)
      }
      response
    }
  }
}
