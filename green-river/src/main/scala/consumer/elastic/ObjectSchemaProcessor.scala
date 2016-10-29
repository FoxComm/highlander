package consumer.elastic

import scala.concurrent.{ExecutionContext, Future}

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings._
import com.sksamuel.elastic4s.mappings.attributes._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import consumer.{AvroJsonHelper, JsonProcessor}
import org.elasticsearch.common.settings.Settings
import org.json4s.DefaultFormats
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods.parse

case class EsOptions(typed: Option[String],
                     name: Option[String],
                     index: Option[String],
                     analyzer: Option[String],
                     format: Option[String]) {

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

case class EsAttributeDefinition(path: Seq[String], es_opts: EsOptions)

/**
  * This is a ObjectSchemaProcessor which processes json with ES schema definition and
  * update mappings in ES
  */
class ObjectSchemaProcessor(uri: String, cluster: String, schemasTopic: String)(
    implicit ec: ExecutionContext)
    extends JsonProcessor {

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client   = ElasticClient.transport(settings, ElasticsearchClientUri(uri))

  implicit val formats: DefaultFormats.type = DefaultFormats

  private val futureUnit: Future[Unit] = Future { () }

  val indexPrefix = "admin" // FIXME: move to settings?


  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {
    val document = AvroJsonHelper.transformJsonRaw(inputJson)

    val esIndex          = (document \ "esIndex").extract[String]
    val schemaAttributes = parse((document \ "schemaAttributes").extract[String])
    val esAttributes     = parse((document \ "esAttributes").extract[String])

    val fieldsDefinition = makeMappingFromJsonSchema(schemaAttributes, esAttributes)

    val scopes = parse((document \ "scopes").extract[String]).extract[List[String]]

    val esFutures = scopes.map { scope ⇒
      Console.out.println(s"SCHEMAS: UPDATE MAPPING admin_$scope with $fieldsDefinition")
      client.execute {
        put mapping s"${indexPrefix}_$scope" / esIndex fields fieldsDefinition
      }.map { _ ⇒
        ()
      }
    }

    Future.sequence(esFutures).map(_ ⇒ ())
  }

  private def makeMappingFromJsonSchema(
      schemaAttributes: JValue, esAttributes: JValue): Seq[TypedFieldDefinition] = {
    val esAttributeDefitions = esAttributes.extract[Seq[EsAttributeDefinition]]
    val props   = schemaAttributes \ "properties"
    esAttributeDefitions.map { attr ⇒
      val schemaDefinition = attr.path.foldLeft(props) {
        case (acc, el) ⇒
          acc \ el
      }
      schemaToEsDefinition(attr, schemaDefinition)
    }
  }

  private def schemaToEsDefinition(attr: EsAttributeDefinition, schema: JValue): TypedFieldDefinition = {
    val name = attr.es_opts.name.getOrElse(attr.path.last)
    val esType = getTypeFromSchemaOrAttr(attr, schema)
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

    attr.es_opts.applyToField(esField)
  }

  private def getTypeFromSchemaOrAttr(attr: EsAttributeDefinition, schema: JValue): String = {
    attr.es_opts.typed.getOrElse {
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
        case "array" ⇒ "string"
        case "boolean" ⇒ "boolean"
        case "integer" ⇒ "integer"
        case "number" ⇒ "float"
        case "object" ⇒ "object"
        case "string" ⇒ "string"
        case x ⇒ throw new IllegalArgumentException(s"Unkown schema type $x")
      }
    }
    schemaType match {
      case JArray(values) ⇒
        // for example, it can be ["string", "null"]
        val types = values.map(_.extract[String]).filter(_ == "null")
        types match {
          case head :: _ ⇒ mapSchemaTypeToEs(head)
          case _ ⇒ throw new IllegalArgumentException(s"schema type $values is not supported")
        }
      case JString(value) ⇒ mapSchemaTypeToEs(value)
      case _ ⇒ throw new IllegalArgumentException(s"Not supported schema type $schemaType")
    }
  }
}
