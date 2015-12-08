package consumer

import scala.concurrent.ExecutionContext

import com.sksamuel.elastic4s.{EdgeNGramTokenFilter, LowercaseTokenFilter, StandardTokenizer,
CustomAnalyzerDefinition, ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.indices.IndexMissingException

import org.json4s.JsonAST.{JInt, JObject, JField, JValue, JString}
import org.json4s.jackson.JsonMethods._

import ElasticSearchMappings._

/**
 * This is a JsonProcessor which processes json and indexs it into elastic search.
 * It calls a json transform function before sending it to elastic search.
 *
 * If the json has a {"id" : <id>} field after transformation, it extracts that
 * id and uses it as the _id in elasticsearch for that item. This is important so that
 * we don't duplicate entries in ES.
 */
class ElasticSearchProcessor(uri: String, cluster: String, indexName: String, topics: Seq[String])
  extends JsonProcessor {

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", cluster).build()
  val client = ElasticClient.remote(settings, ElasticsearchClientUri(uri))

  def beforeAction()(implicit ec: ExecutionContext): Unit = {

    try {
      println(s"Deleting index $indexName...")
      client.execute(deleteIndex(indexName)).await
    } catch {
      case e: IndexMissingException ⇒ Console.err.println(s"Index already exists")
      case e: Throwable             ⇒ Console.err.println(s"Error when dropping index: ${e.getMessage}")
    }

    println("Creating index and type mappings...")

    client.execute {
      create index indexName mappings (
        ElasticSearchMappings.countries,
        ElasticSearchMappings.regions,
        ElasticSearchMappings.customers,
        ElasticSearchMappings.orders
      ) analysis
        CustomAnalyzerDefinition(
          "autocomplete",
          StandardTokenizer,
          LowercaseTokenFilter,
          EdgeNGramTokenFilter("autocomplete_filter", 1, 20)
        )
    }.await()
  }

  def process(offset: Long, topic: String, inputJson: String)(implicit ec: ExecutionContext): Unit = {
    ElasticSearchProcessor.topicInfo(topic) match {
      case Some(jsonFields) ⇒ 
        val document = ElasticSearchProcessor.transformJson(inputJson, jsonFields)
        save(document, topic)
      case _ ⇒ 
        println(s"Skipping information from topic $topic")      
    }
  }

  private def save(document: String, topic: String): Unit = {
    println()
    // See if it has an id and use that as _id in elasticsearch.
    parse(document) \ "id" match {
      case JInt(jid) ⇒
        println(s"Indexing document with ID $jid from topic $topic...\r\n$document")
        client.execute {
          index into indexName / topic id jid doc PassthroughSource(document)
        }.await()

      case _ ⇒
        println(s"Skipping unidentified document from topic $topic...\r\n$document")
    }
  }
}

object ElasticSearchProcessor {

  val customerJsonFields = List("orders", "purchased_items", "shipping_addresses", "billing_addresses", 
    "save_for_later")

  val orderJsonFields = List("customer", "line_items", "payments", "shipments", "shipping_addresses", 
    "billing_addresses", "assignees", "rmas")

  def topicInfo(topic: String): Option[List[String]] = {
    topic match {
      case "countries"                ⇒ Some(List.empty)
      case "regions"                  ⇒ Some(List.empty)
      case "customers_search_view"    ⇒ Some(customerJsonFields)
      case "orders_search_view"       ⇒ Some(orderJsonFields)
      case _                          ⇒ None
    }    
  }

  def transformJson(json: String, jsonList: List[String]): String = {
    // Reduce Avro type annotations
    val unwrapTypes = parse(json).transformField {
      case JField(name, (JObject(JField(typeName, value) :: Nil))) ⇒ (name, value)
    }

    // Convert escaped json fields to AST
    val unescapeJson = unwrapTypes.transformField {
      case JField(name, JString(text)) if jsonList.contains(name) ⇒ (name, parse(text))
    }

    compact(render(unescapeJson))
  }
}