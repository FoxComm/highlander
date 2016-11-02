package consumer.elastic

import scala.concurrent.{ExecutionContext, Future}

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings._
import com.sksamuel.elastic4s.mappings.attributes._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import consumer.{AvroJsonHelper, JsonProcessor}
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.apache.avro.SchemaBuilder
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.DefaultFormats
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods.parse

/**
  Optional options for ES for attribute like index, type, different name, etc
  */
case class EsOptions(typed: Option[String] = None,
                     name: Option[String] = None,
                     index: Option[String] = None,
                     analyzer: Option[String] = None,
                     format: Option[String] = None) {

  val applyToField = applyIndex _ compose applyFormat compose applyAnalyzer

  def applyIndex(field: TypedFieldDefinition): TypedFieldDefinition =
    index.fold(field) { v ⇒
      field match {
        case f: AttributeIndex ⇒
          f.index(v)
        case _ ⇒ field
      }
    }

  def applyFormat(field: TypedFieldDefinition): TypedFieldDefinition =
    format.fold(field) { v ⇒
      field match {
        case f: AttributeFormat ⇒
          f.format(v)
        case _ ⇒ field
      }
    }

  def applyAnalyzer(field: TypedFieldDefinition): TypedFieldDefinition =
    analyzer.fold(field) { v ⇒
      field match {
        case f: AttributeAnalyzer ⇒
          f.analyzer(v)
        case _ ⇒ field
      }
    }
}

/**
  * This is a ObjectSchemaProcessor which processes json with ES schema definition and
  * update mappings in ES
  */
class ObjectSchemaProcessor(
    uri: String, cluster: String, schemasTopic: String, schemaRegistryUrl: String)(
    implicit ec: ExecutionContext)
    extends JsonProcessor {

  import ObjectSchemaProcessor._

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client   = ElasticClient.transport(settings, ElasticsearchClientUri(uri))

  val schemaRegistry = new CachedSchemaRegistryClient(schemaRegistryUrl, 100)

  implicit val formats: DefaultFormats.type = DefaultFormats

  val indexPrefix = "admin" // FIXME: move to settings?

  private def extractOptions(rawOptions: JValue): Map[String, EsOptions] = {
    val default = Map[String, EsOptions]()
    rawOptions match {
      case JString(v) ⇒ parse(v).extractOrElse[Map[String, EsOptions]](default)
      case _          ⇒ default
    }
  }

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {
    val document = AvroJsonHelper.transformJsonRaw(inputJson)

    val esMappingName    = (document \ "esMapping").extract[String]
    val schemaAttributes = parse((document \ "schemaAttributes").extract[String])
    val esAttributes     = parse((document \ "attributes").extract[String]).extract[List[String]]
    val esOptions        = extractOptions(document \ "esOptions")

    registerSchemaAttributes(esMappingName, esAttributes)

    val fieldsDefinition = makeMappingFromJsonSchema(schemaAttributes, esAttributes, esOptions)

    val scopes = parse((document \ "scopes").extract[String]).extract[List[String]]

    val esFutures = scopes.map { scope ⇒
      val index = s"${indexPrefix}_$scope"
      Console.out.println(s"SCHEMAS: Update mapping $index with $fieldsDefinition")
      client.execute {
        put mapping index / esMappingName fields fieldsDefinition
      }.recover {
        case _: IndexNotFoundException ⇒
          Console.err.println(s"Index $index not found, skip")
        case r: RemoteTransportException if r.getCause.isInstanceOf[IndexNotFoundException] ⇒
          Console.err.println(s"Index $index not found, skip")
      }.map { _ ⇒
        ()
      }
    }

    Future.sequence(esFutures).map(_ ⇒ ())
  }

  private def makeMappingFromJsonSchema(
      schemaAttributes: JValue,
      esAttributes: Seq[String],
      esOptions: Map[String, EsOptions]): Seq[TypedFieldDefinition] = {

    val schemaProps = schemaAttributes \ "properties"
    esAttributes.map { attributeName ⇒
      val fieldOptions     = esOptions.getOrElse(attributeName, EsOptions())
      val schemaDefinition = schemaProps \ attributeName

      schemaToEsDefinition(attributeName, schemaDefinition, fieldOptions)
    }
  }

  private def schemaToEsDefinition(
      attributeName: String, schema: JValue, esOptions: EsOptions): TypedFieldDefinition = {
    val name   = esOptions.name.getOrElse(attributeName)
    val esType = getTypeFromSchemaOrAttr(esOptions, schema)
    val esField: TypedFieldDefinition = esType match {
      case "attachment"  ⇒ new AttachmentFieldDefinition(name)
      case "binary"      ⇒ new BinaryFieldDefinition(name)
      case "boolean"     ⇒ new BooleanFieldDefinition(name)
      case "byte"        ⇒ new ByteFieldDefinition(name)
      case "completion"  ⇒ new CompletionFieldDefinition(name)
      case "date"        ⇒ new DateFieldDefinition(name)
      case "double"      ⇒ new DoubleFieldDefinition(name)
      case "float"       ⇒ new FloatFieldDefinition(name)
      case "integer"     ⇒ new IntegerFieldDefinition(name)
      case "ip"          ⇒ new IpFieldDefinition(name)
      case "geo_point"   ⇒ new GeoPointFieldDefinition(name)
      case "geo_shape"   ⇒ new GeoShapeFieldDefinition(name)
      case "long"        ⇒ new LongFieldDefinition(name)
      case "multi_field" ⇒ new MultiFieldDefinition(name)
      case "nested"      ⇒ new NestedFieldDefinition(name)
      case "object"      ⇒ new ObjectFieldDefinition(name)
      case "short"       ⇒ new ShortFieldDefinition(name)
      case "string"      ⇒ new StringFieldDefinition(name)
      case "token_count" ⇒ new TokenCountDefinition(name)
      case x             ⇒ throw new IllegalArgumentException(s"Unknown ES type $x")
    }

    esOptions.applyToField(esField)
  }

  private def getTypeFromSchemaOrAttr(esOptions: EsOptions, schema: JValue): String = {
    esOptions.typed.getOrElse {
      guessEsTypeFromSchemaType(schema \ "type")
    }
  }
  private def guessEsTypeFromSchemaType(schemaType: JValue): String = {
    // Json schema types: http://json-schema.org/latest/json-schema-core.html#anchor8
    //    array, boolean, integer, number
    //     Any JSON number. Number includes integer.
    //    null, object, string
    def mapSchemaTypeToEs(schemaType: String): String = {
      schemaType match {
        case "array"   ⇒ "string"
        case "boolean" ⇒ "boolean"
        case "integer" ⇒ "integer"
        case "number"  ⇒ "float"
        case "object"  ⇒ "object"
        case "string"  ⇒ "string"
        case x         ⇒ throw new IllegalArgumentException(s"Unknown schema type $x")
      }
    }
    schemaType match {
      case JArray(values) ⇒
        // for example, it can be ["string", "null"]
        val types = values.map(_.extract[String]).filter(_ != "null")
        types match {
          case head :: _ ⇒ mapSchemaTypeToEs(head)
          case _         ⇒ throw new IllegalArgumentException(s"schema type $values is not supported")
        }
      case JString(value) ⇒ mapSchemaTypeToEs(value)
      case _              ⇒ throw new IllegalArgumentException(s"Not supported schema type $schemaType")
    }
  }

  private def registerSchemaAttributes(esMapping: String, attributes: Seq[String]) = {
    val avroSchemaName = getSchemaAttributesAvroName(esMapping)
    val fields         = SchemaBuilder.record(avroSchemaName).fields()

    val schema = attributes
      .foldLeft(fields) {
        case (schemaBuilder, attr) ⇒
          schemaBuilder.name(attr).`type`().nullable().stringType().noDefault()
      }
      .endRecord()
    schemaRegistry.register(avroSchemaName, schema)
  }
}

object ObjectSchemaProcessor {
  def getSchemaAttributesAvroName(esMapping: String): String =
    s"attributes_${esMapping}"
}
