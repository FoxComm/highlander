package consumer.elastic

import scala.concurrent.ExecutionContext

import com.sksamuel.elastic4s.{EdgeNGramTokenFilter, LowercaseTokenFilter, StandardTokenizer,
CustomAnalyzerDefinition, ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.MappingDefinition
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.indices.IndexMissingException
import org.elasticsearch.transport.RemoteTransportException

import org.json4s.JsonAST.JInt
import org.json4s.jackson.JsonMethods.parse

import consumer.JsonProcessor
import consumer.PassthroughSource

/**
 * Json transformer has two parts, the ES mapping definition and 
 * a function that takes json and transforms it to another json 
 * before it is saved to ES
 */
trait JsonTransformer { 
  def mapping() : MappingDefinition
  def transform(json: String) : String
}

/**
 * This is a JsonProcessor which processes json and indexs it into elastic search.
 * It calls a json transform function before sending it to elastic search.
 *
 * If the json has a {"id" : <id>} field after transformation, it extracts that
 * id and uses it as the _id in elasticsearch for that item. This is important so that
 * we don't duplicate entries in ES.
 */
class ElasticSearchProcessor(
  uri: String, 
  cluster: String, 
  indexName: String, 
  topics: Seq[String],
  jsonTransformers: Map[String, JsonTransformer])
(implicit ec: ExecutionContext)
  extends JsonProcessor {

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", cluster).build()
  val client = ElasticClient.remote(settings, ElasticsearchClientUri(uri))


  def beforeAction(): Unit = {
    removeIndex()
    createIndex()
  }

  def process(offset: Long, topic: String, inputJson: String): Unit = {
    //find json transformer
    jsonTransformers get topic match {
      case Some(t) ⇒
        val outJson = t.transform(inputJson)
        save(outJson, topic)
      case None ⇒ 
        println(s"Skipping information from topic $topic")      
    }
  }

  private def removeIndex() { 
    try {
      println(s"Deleting index $indexName...")
      client.execute(deleteIndex(indexName)).await
    } catch {
      case e: IndexMissingException ⇒ Console.err.println(s"Index already exists")
    }
  }

  private def createIndex() {
    println("Creating index and type mappings...")
    //define mappings an analyzer
    val jsonMappings = jsonTransformers.mapValues(_.mapping()).values.toSeq
    val customAnalyzer =
      CustomAnalyzerDefinition(
        "autocomplete",
        StandardTokenizer,
        LowercaseTokenFilter,
        EdgeNGramTokenFilter("autocomplete_filter", 1, 20)
      )

    //execute es query
    client.execute {
      create index indexName mappings (jsonMappings: _*) analysis customAnalyzer
    }.await()
  }

  private def save(document: String, topic: String): Unit = {
    println()
    // See if it has an id and use that as _id in elasticsearch.
    parse(document) \ "id" match {
      case JInt(jid) ⇒
        try {
          println(s"Indexing document with ID $jid from topic $topic...\r\n$document")

          client.execute {
            index into indexName / topic id jid doc PassthroughSource(document)
          }.await()
        } catch {
          case e: RemoteTransportException ⇒ Console.err.println(s"Error while indexing: $e")
          case e: Throwable ⇒ Console.err.println(s"Error while indexing: $e")
        }

      case _ ⇒
        println(s"Skipping unidentified document from topic $topic...\r\n$document")
    }
  }

}
