package consumer.elastic

import scala.util.Try
import scala.collection.JavaConverters._

import consumer.aliases.Json
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.apache.avro.SchemaBuilder
import org.json4s.DefaultFormats
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import utils.objects.Illuminated

/**
  * Designed for set custom attributes for documents
  * defined by ObjectSchemas
  */
object ObjectAttributesTransformer {

  implicit val formats: DefaultFormats.type = DefaultFormats

  private def getAvroName(esTopic: String): String = s"attributes_${esTopic}"

  private def getCustomAttributes(topic: String)(
      implicit schemaRegistry: CachedSchemaRegistryClient): Option[List[String]] = {
    val avroSchemaName = getAvroName(topic)

    Try {
      val schemaMeta = schemaRegistry.getLatestSchemaMetadata(avroSchemaName)
      val schema     = schemaRegistry.getByID(schemaMeta.getId)
      schema.getFields.asScala.map(_.name).toList
    }.toOption
  }

  def registerCustomAttributes(esMapping: String, attributes: Seq[String])(
      implicit schemaRegistry: CachedSchemaRegistryClient) = {
    val avroSchemaName = getAvroName(esMapping)
    val fields         = SchemaBuilder.record(avroSchemaName).fields()

    val schema = attributes
      .foldLeft(fields) {
        case (schemaBuilder, attr) ⇒
          schemaBuilder.name(attr).`type`().nullable().stringType().noDefault()
      }
      .endRecord()
    schemaRegistry.register(avroSchemaName, schema)
  }

  def enrichDocument(document: String, topic: String)(
      implicit schemaRegistryClient: CachedSchemaRegistryClient): Json = {
    val originalJson = parse(document)
    enrichDocument(originalJson, topic)
  }

  /**
    * Enrich Json document
    * by getting additional fields from (form / shadow)
    * using attributes names stored in Schema registry
    * Also removes form and shadow from original document
    */
  def enrichDocument(originalJson: Json, topic: String)(
      implicit schemaRegistryClient: CachedSchemaRegistryClient): Json = {
    val json = originalJson.removeField {
      case ("form", _)   ⇒ true
      case ("shadow", _) ⇒ true
      case _             ⇒ false
    }

    json match {
      case jsonObject: JObject ⇒
        (for {
          formString   ← (originalJson \ "form").extractOpt[String]
          shadowString ← (originalJson \ "shadow").extractOpt[String]
          attributes   ← getCustomAttributes(topic)
          form   = parse(formString)
          shadow = parse(shadowString)
        } yield
          attributes.foldLeft(jsonObject) {
            case (j, attr) ⇒
              j ~ (attr → Illuminated.get(attr, form, shadow))
          }).getOrElse(json)
      case _ ⇒ json
    }
  }
}
