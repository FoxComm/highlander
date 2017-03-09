package consumer

import consumer.aliases._
import consumer.utils.JsonTransformers
import java.io.ByteArrayOutputStream

import scala.concurrent.Future

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.AbstractKafkaAvroDeserializer
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.io.EncoderFactory
import org.apache.kafka.common.errors.SerializationException

import org.json4s.JsonAST.{JValue, JObject, JField, JString}
import org.json4s.jackson.JsonMethods.{render, compact, parse}

import scala.util.control.NonFatal

/**
  * Reads kafka processor that reads expects messages in kafka to be from bottledwater-pg
  * which are serialized using Avro.
  *
  * https://github.com/confluentinc/schema-registry
  *
  * It then converts the avro to json and gives whatever json processor you give it that
  * json to work with.
  */
class AvroProcessor(schemaRegistryUrl: String, processor: JsonProcessor)(implicit ec: EC)
    extends AbstractKafkaAvroDeserializer
    with MessageProcessor {

  this.schemaRegistry = new CachedSchemaRegistryClient(
      schemaRegistryUrl, DEFAULT_MAX_SCHEMAS_PER_SUBJECT)
  val encoderFactory = EncoderFactory.get()

  def process(offset: Long, topic: String, key: Array[Byte], message: Array[Byte]): Future[Unit] = {
    try {

      val keyJson =
        if (key == null || key.isEmpty) {
          Console.err.println(
              s"Warning, message has no key for topic ${topic}: ${new String(message, "UTF-8")}")
          ""
        } else {
          deserializeAvro(key)
        }

      val json = deserializeAvro(message)

      processor.process(offset, topic, keyJson, json)
    } catch {
      case e: SerializationException ⇒
        Future.failed {
          val readableKey     = new String(key, "UTF-8")
          val readableMessage = new String(message, "UTF-8")
          Console.err.println(
              s"Error deserializing avro message with key $readableKey: error $e\n\t$readableMessage")
          e
        }
      case e: Throwable ⇒ Future.failed(e)
    }
  }

  def deserializeAvro(v: Array[Byte]): String = {
    val obj     = deserialize(v)
    val schema  = getSchema(obj)
    val stream  = new ByteArrayOutputStream
    val encoder = this.encoderFactory.jsonEncoder(schema, stream)
    val writer  = new GenericDatumWriter[Object](schema)
    writer.write(obj, encoder)
    encoder.flush()
    new String(stream.toByteArray, "UTF-8")
  }
}

/**
  * Helper functions to transform json comming from bottledwater into something
  * more reasonable.
  */
object AvroJsonHelper {

  def transformJson(json: String, fields: List[String] = List.empty): String = {
    compact(render(transformJsonRaw(json, fields)))
  }

  def transformJsonRaw(json: String, fields: List[String] = List.empty): JValue = {
    JsonTransformers.camelCase(stringToJson(deannotateAvroTypes(parse(json)), fields))
  }

  private def convertType(typeName: String, value: JValue): JValue =
    typeName match {
      case "com.martinkl.bottledwater.datatypes.DateTime" ⇒
        JsonTransformers.dateTimeToDateString(value)
      case _ ⇒ value
    }

  private def deannotateAvroTypes(input: JValue): JValue = {
    input.transformField {
      case JField(name, (JObject(JField(typeName, value) :: Nil))) ⇒ {
          (name, convertType(typeName, value))
        }
    }
  }

  private def stringToJson(input: JValue, fields: List[String]): JValue = {
    input.transformField {
      case JField(name, JString(text)) if fields.contains(name) ⇒ {
          // Try to parse the text as json, otherwise treat it as text
          try {
            (name, parse(text))
          } catch {
            case NonFatal(e) ⇒
              Console.println(s"Error during parsing field $name: ${e.getMessage}")
              (name, JString(text))
          }
        }
    }
  }
}
