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
        "countries" as (
          "id"        typed IntegerType,
          "name"      typed StringType analyzer "autocomplete",
          "continent" typed StringType index "not_analyzed",
          "currency"  typed StringType index "not_analyzed"
        ),
        "regions" as (
          "id"          typed IntegerType,
          "country_id"  typed IntegerType,
          "name"        typed StringType analyzer "autocomplete"
        ),
        "customers_search_view" as (
          "id"                    typed IntegerType,
          "name"                  typed StringType analyzer "autocomplete",
          "email"                 typed StringType analyzer "autocomplete",
          "is_disabled"           typed BooleanType,
          "is_guest"              typed BooleanType,
          "is_blacklisted"        typed BooleanType,
          "date_joined"           typed DateType,
          "revenue"               typed IntegerType,
          "rank"                  typed IntegerType,
          "store_credit_total"    typed IntegerType,
          "store_credit_count"    typed IntegerType,
          "orders"                nested (
            "reference_number" typed StringType analyzer "autocomplete",
            "status"           typed StringType index "not_analyzed",
            "date_placed"      typed DateType
          ),
          "order_count"           typed IntegerType,
          "purchased_items"       nested (
            "sku"   typed StringType analyzer "autocomplete",
            "name"  typed StringType analyzer "autocomplete",
            "price" typed IntegerType
          ),
          "purchased_items_count" typed IntegerType,
          "shipping_addresses"    nested (
            "address1"          typed StringType analyzer "autocomplete",
            "address2"          typed StringType analyzer "autocomplete",
            "city"              typed StringType analyzer "autocomplete",
            "zip"               typed StringType index "not_analyzed",
            "region_name"       typed StringType analyzer "autocomplete",
            "country_name"      typed StringType analyzer "autocomplete",
            "country_continent" typed StringType analyzer "autocomplete",
            "country_currency"  typed  StringType analyzer "autocomplete"
          ),
          "billing_addresses"     nested (
            "address1"          typed StringType analyzer "autocomplete",
            "address2"          typed StringType analyzer "autocomplete",
            "city"              typed StringType analyzer "autocomplete",
            "zip"               typed StringType index "not_analyzed",
            "region_name"       typed StringType analyzer "autocomplete",
            "country_name"      typed StringType analyzer "autocomplete",
            "country_continent" typed StringType analyzer "autocomplete",
            "country_currency"  typed  StringType analyzer "autocomplete"
          ),
          "saved_for_later"       nested (
            "sku"   typed StringType analyzer "autocomplete",
            "name"  typed StringType analyzer "autocomplete",
            "price" typed IntegerType
          ),
          "saved_for_later_count" typed IntegerType
        )
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

  def topicInfo(topic: String): Option[List[String]] = {
    topic match {
      case "countries"                ⇒ Some(List.empty)
      case "regions"                  ⇒ Some(List.empty)
      case "customers_search_view"    ⇒ Some(List("orders", "purchased_items", "shipping_addresses", 
        "billing_addresses", "save_for_later"))
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