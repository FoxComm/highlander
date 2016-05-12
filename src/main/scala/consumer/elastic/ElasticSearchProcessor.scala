package consumer.elastic

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import consumer.JsonProcessor
import consumer.PassthroughSource

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.client.transport.NoNodeAvailableException
import org.elasticsearch.transport.RemoteTransportException

import org.json4s.JsonAST.JInt
import org.json4s.jackson.JsonMethods.parse

/**
 * This is a JsonProcessor which processes json and indexs it into elastic search.
 * It calls a json transform function before sending it to elastic search.
 *
 * If the json has a {"id" : <id>} field after transformation, it extracts that
 * id and uses it as the _id in elasticsearch for that item. This is important so that
 * we don't duplicate entries in ES.
 */
class ElasticSearchProcessor(uri: String, cluster: String, indexName: String, topics: Seq[String],
  jsonTransformers: Map[String, JsonTransformer])(implicit ec: ExecutionContext) extends JsonProcessor {

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client = ElasticClient.transport(settings, ElasticsearchClientUri(uri))

  def createMappings(): Unit = {
    removeIndex()
    createIndex()
  }

  def process(offset: Long, topic: String, inputJson: String): Future[Unit] = {
    // Find json transformer
    jsonTransformers get topic match {
      case Some(t) ⇒
        t.transform(inputJson).map{
          outJson ⇒
            save(outJson, topic)
        }
      case None ⇒ 
        Console.out.println(s"Skipping information from topic $topic")
        Future {()}
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
      val jsonMappings = jsonTransformers.mapValues(_.mapping()).values.toSeq
      val customAnalyzer =
        CustomAnalyzerDefinition(
          "autocomplete",
          EdgeNGramTokenizer("autocomplete_tokenizer", 1, 20, Seq("letter", "digit", "punctuation", "symbol")),
          LowercaseTokenFilter
        )

      // Execute Elasticsearch query
      client.execute {
        create index indexName mappings (jsonMappings: _*) analysis customAnalyzer
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

  private def save(document: String, topic: String): Future[Unit] = {
    // See if it has an id and use that as _id in elasticsearch.
    parse(document) \ "id" match {
      case JInt(jid) ⇒
        Console.out.println(s"Indexing document with ID $jid from topic $topic...\r\n$document")

        val req = client.execute {
          index into indexName / topic id jid doc PassthroughSource(document)
        }

        req onFailure {
          case NonFatal(e) ⇒ Console.err.println(s"Error while indexing: $e")
        }

        req.map{ r ⇒ ()}
      case _ ⇒
        Console.out.println(s"Skipping unidentified document from topic $topic...\r\n$document")
        Future { () }
    }
  }
}
