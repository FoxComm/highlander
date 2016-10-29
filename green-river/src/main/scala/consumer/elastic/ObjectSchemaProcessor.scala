package consumer.elastic

import scala.collection.mutable.Buffer
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

/**
  * This is a JsonProcessor which processes json and indexs it into elastic search.
  * It calls a json transform function before sending it to elastic search.
  *
  * If the json has a {"id" : <id>} field after transformation, it extracts that
  * id and uses it as the _id in elasticsearch for that item. This is important so that
  * we don't duplicate entries in ES.
  */
case class EsOptions(typed: String,
                     name: Option[String],
                     index: Option[String],
                     analyzer: Option[String],
                     format: Option[String]) {

  val applyToField = applyIndex _ compose applyFormat compose applyAnalyzer

  def applyIndex(field: TypedFieldDefinition): TypedFieldDefinition = applyInner(field, index)

  def applyFormat(field: TypedFieldDefinition): TypedFieldDefinition = applyInner(field, format)

  def applyAnalyzer(field: TypedFieldDefinition): TypedFieldDefinition =
    applyInner(field, analyzer)

  private def applyInner(field: TypedFieldDefinition, option: Option[String]) =
    option.fold(field) { value ⇒
      applyMatch(field, value)
    }

  private def applyMatch(field: TypedFieldDefinition, value: String): TypedFieldDefinition = {
    field match {
      case f: AttributeIndex    ⇒ f.index(value)
      case f: AttributeFormat   ⇒ f.format(value)
      case f: AttributeAnalyzer ⇒ f.analyzer(value)
      case _                    ⇒ field
    }
  }
}

case class EsAttribute(path: Seq[String], es_opts: EsOptions)

class ObjectSchemaProcessor(uri: String, cluster: String, schemasTopic: String)(
    implicit ec: ExecutionContext)
    extends JsonProcessor {

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client   = ElasticClient.transport(settings, ElasticsearchClientUri(uri))

  implicit val formats: DefaultFormats.type = DefaultFormats

  private val futureUnit: Future[Unit] = Future { () }

  val indexPrefix = "admin" // FIXME: move to settings?

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {
    var createFutures = Buffer[Future[Unit]]()

    val parsed   = AvroJsonHelper.transformJson(inputJson)
    val document = AvroJsonHelper.transformJsonRaw(inputJson)

    Console.out.println(s"SCHEMAS: parsed = $parsed")
    val esIndex          = (document \ "esIndex").extract[String]
    val schemaAttributes = parse((document \ "schemaAttributes").extract[String])
    val esAttributes     = parse((document \ "esAttributes").extract[String])

    val mapping          = makeMappingFromJsonSchema(schemaAttributes, esAttributes)
    val fieldsDefinition = makeMappingFromJsonSchema(schemaAttributes, esAttributes)

    val scopes = parse((document \ "scopes").extract[String]).extract[List[String]]

    scopes.foreach { scope ⇒
      Console.out.println(s"SCHEMAS: UPDATE MAPPING admin_$scope with $fieldsDefinition")
      createFutures += client.execute {
        put mapping s"${indexPrefix}_$scope" / esIndex fields fieldsDefinition
      }.map { _ ⇒
        ()
      }
    }

    Future.sequence(createFutures).map(_ ⇒ ())
  }

  private def makeMappingFromJsonSchema(
      schemaAttributes: JValue, esAttributes: JValue): Seq[TypedFieldDefinition] = {
    val esAttrs = esAttributes.extract[Seq[EsAttribute]]
    val props   = schemaAttributes \ "properties"
    esAttrs.map { attr ⇒
      val schemaDefinition = attr.path.foldLeft(props) {
        case (acc, el) ⇒
          acc \ el
      }
      schemaToEsDefinition(attr, schemaDefinition)
    }
  }

  private def schemaToEsDefinition(attr: EsAttribute, schema: JValue): TypedFieldDefinition = {
    val name = attr.es_opts.name.getOrElse(attr.path.last)
    val esField: TypedFieldDefinition = attr.es_opts.typed match {
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
}
