package consumer

import java.io.ByteArrayOutputStream

import scala.concurrent.ExecutionContext

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.AbstractKafkaAvroDeserializer
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.io.EncoderFactory

import org.json4s.JsonAST.{JInt, JObject, JField, JString}
import org.json4s.jackson.JsonMethods._

/**
 * Reads kafka processor that reads expects messages in kafka to be from bottledwater-pg
 * which are serialized using Avro.
 *
 * https://github.com/confluentinc/schema-registry
 *
 * It then converts the avro to json and gives whatever json processor you give it that
 * json to work with.
 */
class AvroProcessor(schemaRegistryUrl: String, processor: JsonProcessor)
  extends AbstractKafkaAvroDeserializer
  with MessageProcessor {

  this.schemaRegistry = new CachedSchemaRegistryClient(schemaRegistryUrl, DEFAULT_MAX_SCHEMAS_PER_SUBJECT)
  val encoderFactory = EncoderFactory.get()

  def process(offset: Long, topic: String, message: Array[Byte])(implicit ec: ExecutionContext) {
    try {
      val stream = new ByteArrayOutputStream
      val obj = deserialize(message)
      val schema = getSchema(obj)
      val encoder = this.encoderFactory.jsonEncoder(schema, stream)
      val writer = new GenericDatumWriter[Object](schema)
      writer.write(obj, encoder)
      encoder.flush()

      val json = new String(stream.toByteArray, "UTF-8")
      processor.process(offset, topic, json)

    } catch {
      case e: Throwable ⇒ Console.err.println(s"Error consuming avro message $e")
    }
  }
}

/**
 * Helper functions to transform json comming from bottledwater into something
 * more reasonable.
 */
object AvroJsonHelper { 
  def transformJson(json: String, fields: Map[String, String]): String = {
    // Reduce Avro type annotations
    val unwrapTypes = parse(json).transformField {
      case JField(name, (JObject(JField(typeName, value) :: Nil))) ⇒ (name, value)
    }

    // Convert escaped json fields to AST
    val unescapeJson = unwrapTypes.transformField {
      case JField(name, JString(text)) if fields.contains(name) ⇒ (fields(name), parse(text))
    }

    compact(render(unescapeJson))
  }
}
