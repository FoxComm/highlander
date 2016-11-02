package consumer.elastic

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import consumer.{JsonProcessor, PassthroughSource}
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.elasticsearch.common.settings.Settings
import org.json4s.DefaultFormats
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.{compact, parse}

/**
  * This is a JsonProcessor which processes json and indexs it into elastic search.
  * It calls a json transform function before sending it to elastic search.
  *
  * If the json has a {"id" : <id>} field after transformation, it extracts that
  * id and uses it as the _id in elasticsearch for that item. This is important so that
  * we don't duplicate entries in ES.
  */
class ScopedIndexer(uri: String,
                    cluster: String,
                    indexName: String,
                    topics: Seq[String],
                    jsonTransformers: Map[String, JsonTransformer],
                    schemaRegistryUrl: String)(implicit ec: ExecutionContext)
    extends JsonProcessor {

  val settings                              = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client                                = ElasticClient.transport(settings, ElasticsearchClientUri(uri))
  val schemaRegistry                        = new CachedSchemaRegistryClient(schemaRegistryUrl, 100)
  implicit val formats: DefaultFormats.type = DefaultFormats

  type Json = JValue

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {
    // Find json transformer
    jsonTransformers get topic match {
      case Some(t) ⇒
        t.transform(inputJson).flatMap { outJson ⇒
          save(outJson, topic)
        }
      case None ⇒
        Console.out.println(s"Skipping information from topic $topic")
        Future { () }
    }
  }

  //This save will index into several indices based on scope. Scopes are
  //constructured as a path with '.' seperating the elements. We want to index
  //based on the scope down the path. For example if the scope is "1.2.3" we will index
  //into 3 indices
  //
  //1.  admin_1.2.3
  //2.  admin_1.2
  //3.  admin_1
  //
  private def save(document: String, topic: String): Future[Unit] = {
    val json        = enrichDocument(document, topic)
    val newDocument = compact(json)

    json \ "id" match {
      case JInt(jid) ⇒
        json \ "scope" match {
          case JString(scope) ⇒ {
              val path = scope.split('.')
              Future
                .sequence((1 to path.length).map { idx ⇒
                  val partialScope    = path.slice(0, idx).mkString(".")
                  val scopedIndexName = s"${indexName}_${partialScope}"
                  save(scopedIndexName, jid, newDocument, topic)
                })
                .map(_ ⇒ ())
            }
          //if no scope found, just save the good old way
          case _ ⇒ save(indexName, jid, newDocument, topic)
        }
      case _ ⇒
        Console.out.println(s"Skipping unidentified document from topic $topic...\r\n$newDocument")
        Future { () }
    }
  }

  private def save(scopedIndexName: String,
                   documentId: BigInt,
                   document: String,
                   topic: String): Future[Unit] = {
    Console.out.println(s"Scoped Indexing $topic into $scopedIndexName")
    val req = client.execute {
      index into scopedIndexName / topic id documentId doc PassthroughSource(document)
    }

    req onFailure {
      case NonFatal(e) ⇒ Console.err.println(s"Error while indexing: $e")
    }

    req.map { _ ⇒
      ()
    }
  }

  private def getAdditionalAttributes(topic: String): Seq[String] = {
    val avroSchemaName = ObjectSchemaProcessor.getSchemaAttributesAvroName(topic)
    Try {
      val schemaMeta = schemaRegistry.getLatestSchemaMetadata(avroSchemaName)
      val schema     = schemaRegistry.getByID(schemaMeta.getId)
      schema.getFields.asScala.map(_.name).toList
    }.toOption.getOrElse(List.empty[String])
  }

  def enrichDocument(document: String, topic: String): JValue = {
    val originalJson = parse(document)
    val form         = originalJson \ "form"
    val shadow       = originalJson \ "shadow"

    val json = originalJson.removeField {
      case ("form", _)   ⇒ true
      case ("shadow", _) ⇒ true
      case _             ⇒ false
    }

    json match {
      case jsonObject: JObject ⇒
        val additionalAttrs = getAdditionalAttributes(topic)
        val jForm           = parse(form.extract[String])
        val jShadow         = parse(shadow.extract[String])

        additionalAttrs.foldLeft(jsonObject) {
          case (j, attr) ⇒
            j ~ (attr → getFormAttr(attr, jForm, jShadow))
        }
      case _ ⇒
        json
    }
  }

  private def getFormAttr(attr: String, form: Json, shadow: Json): Json =
    shadow \ attr \ "ref" match {
      case JString(key) ⇒ form \ key
      case _            ⇒ JNothing
    }
}
