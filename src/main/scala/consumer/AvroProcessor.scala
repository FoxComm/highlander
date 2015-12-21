package consumer

import java.io.ByteArrayOutputStream

import scala.concurrent.ExecutionContext
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
class AvroProcessor(schemaRegistryUrl: String, processor: JsonProcessor)(implicit ec: ExecutionContext) 
  extends AbstractKafkaAvroDeserializer
  with MessageProcessor {

  this.schemaRegistry = new CachedSchemaRegistryClient(schemaRegistryUrl, DEFAULT_MAX_SCHEMAS_PER_SUBJECT)
  val encoderFactory = EncoderFactory.get()

  def process(offset: Long, topic: String, message: Array[Byte]) : Future[Unit] = {
    try {
      val stream = new ByteArrayOutputStream
      val obj = deserialize(message)
      val schema = getSchema(obj)
      val encoder = this.encoderFactory.jsonEncoder(schema, stream)
      val writer = new GenericDatumWriter[Object](schema)
      writer.write(obj, encoder)
      encoder.flush()

      val json = new String(stream.toByteArray, "UTF-8")
      val f = processor.process(offset, topic, json)
      f onFailure { 
        case NonFatal(e) ⇒ Future{Console.err.println(s"Error processing avro message $e")}
      }
      return f
    } catch {
      case e: SerializationException ⇒ Future{Console.err.println(s"Error serializing avro message $e")}
      case NonFatal(e) ⇒ Future{Console.err.println(s"Error processing avro message $e")}
    }
  }
}

/**
 * Helper functions to transform json comming from bottledwater into something
 * more reasonable.
 */
object AvroJsonHelper {
  def transformJson(json: String, fields: List[String] = List.empty): String = {
    val filteredJson = camelCase(stringToJson(deannotateAvroTypes(parse(json)), fields))
    compact(render(filteredJson))
  }

  private def underscoreToCamel(s: String): String = "_([a-z])".r.replaceAllIn(s, _.group(1).toUpperCase)

  private def camelCase(input: JValue): JValue = {
    input.transformField {
      case JField(name, anything) ⇒ (underscoreToCamel(name), anything)
    }
  }

  private def deannotateAvroTypes(input: JValue): JValue = {
    input.transformField {
      case JField(name, (JObject(JField(typeName, value) :: Nil))) ⇒ {
        (name, value)
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
