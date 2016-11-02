package consumer.elastic

import consumer.AvroJsonHelper
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import consumer.JsonProcessor
import consumer.PassthroughSource
import consumer.elastic.mappings._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import consumer.aliases.SRClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.client.transport.NoNodeAvailableException
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.JsonAST.JInt
import org.json4s.jackson.JsonMethods.compact

/**
  * This is a JsonProcessor which processes json and indexs it into elastic search.
  * It calls a json transform function before sending it to elastic search.
  *
  * If the json has a {"id" : <id>} field after transformation, it extracts that
  * id and uses it as the _id in elasticsearch for that item. This is important so that
  * we don't duplicate entries in ES.
  */
class ElasticSearchProcessor(uri: String,
                             cluster: String,
                             indexName: String,
                             topics: Seq[String],
                             jsonTransformers: Map[String, JsonTransformer])(
    implicit ec: ExecutionContext, schemaRegistry: SRClient)
    extends JsonProcessor {

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client   = ElasticClient.transport(settings, ElasticsearchClientUri(uri))

  def createMappings(): Unit = {
    removeIndex()
    createIndex()
  }

  private val futureUnit: Future[Unit] = Future { () }

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] =
    getDocumentId(key, inputJson).fold {
      Console.err.println(
          s"Can't find ID for document $inputJson and key $key for topic $topic, offset = $offset")
      futureUnit
    } { id ⇒
      inputJson match {
        case "null" ⇒ deleteFromIndex(topic, id)
        case _      ⇒
          // Find json transformer
          jsonTransformers get topic match {
            case Some(t) ⇒
              t.transform(inputJson).flatMap { outJson ⇒
                save(outJson, topic, id)
              }
            case None ⇒
              Console.out.println(s"Skipping information from topic $topic with key ${key}")
              futureUnit
          }
      }
    }

  private def deleteFromIndex(topic: String, id: BigInt): Future[Unit] = {
    Console.out.println(s"Deleting document with ID $id from topic $topic")
    val req = client.execute {
      delete id id from indexName / topic
    }

    req onFailure {
      case NonFatal(e) ⇒ Console.err.println(s"Error while deleting: $id")
    }
    req.map { r ⇒
      ()
    }
  }

  private val idFields = List("id")

  private def getDocumentId(keyJson: String, dataJson: String): Option[BigInt] = {
    getIntKey(keyJson) match {
      case None   ⇒ getIntKey(dataJson)
      case someId ⇒ someId
    }
  }

  private def getIntKey(rawJson: String): Option[BigInt] = {
    val idJson = AvroJsonHelper.transformJsonRaw(rawJson, idFields)
    idJson \ "id" match {
      case JInt(id) ⇒ Some(id)
      case _        ⇒ None
    }
  }

  private def removeIndex() {
    try {
      Console.out.println(s"""Deleting index "$indexName"...""")
      client.execute(deleteIndex(indexName)).await
    } catch {
      case e: RemoteTransportException ⇒
        Console.err.println(s"Index already deleted")
      case e: NoNodeAvailableException ⇒
        Console.err.println(s"Error communicating with Elasticsearch: $e")
        System.exit(1)
      case NonFatal(e) ⇒
        Console.err.println(s"Error deleting index, carry on... $e")
        System.exit(1)
    }
  }

  private def createIndex() {
    Console.out.println("Creating index and type mappings...")
    try {
      // Define analyzer in mapping
      val jsonMappings = jsonTransformers.filter {
        case (key, _) ⇒ topics.contains(key)
      }.mapValues(_.mapping()).values.toSeq

      // Execute Elasticsearch query
      client.execute {
        create index indexName mappings (jsonMappings: _*) analysis (autocompleteAnalyzer, lowerCasedAnalyzer, upperCasedAnalyzer)
      }.await
    } catch {
      case e: RemoteTransportException ⇒
        Console.err.println(s"Error connecting to ES: $e")
        System.exit(1)
      case NonFatal(e) ⇒
        Console.err.println(s"Error creating index, carry on... $e")
        System.exit(1)
    }
  }

  private def save(document: String, topic: String, id: BigInt): Future[Unit] = {
    // See if it has an id and use that as _id in elasticsearch.
    Console.out.println(s"Indexing document with ID $id from topic $topic...\r\n$document")

    val json        = ObjectAttributesTransformer.enrichDocument(document, topic)
    val newDocument = compact(json)

    val req = client.execute {
      index into indexName / topic id id doc PassthroughSource(newDocument)
    }

    req onFailure {
      case NonFatal(e) ⇒ Console.err.println(s"Error while indexing: $e")
    }

    req.map { _ ⇒
      ()
    }
  }
}
