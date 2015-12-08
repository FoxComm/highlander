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
          // Customer
          "id"                    typed IntegerType,
          "name"                  typed StringType analyzer "autocomplete",
          "email"                 typed StringType analyzer "autocomplete",
          "is_disabled"           typed BooleanType,
          "is_guest"              typed BooleanType,
          "is_blacklisted"        typed BooleanType,
          "joined_at"             typed DateType,
          "revenue"               typed IntegerType,
          "rank"                  typed IntegerType,
          // Orders
          "order_count"           typed IntegerType,
          "orders"                nested (
            "reference_number"    typed StringType analyzer "autocomplete",
            "status"              typed StringType index "not_analyzed",
            "created_at"          typed DateType,
            "placed_at"           typed DateType,
            "sub_total"           typed IntegerType,
            "shipping_total"      typed IntegerType,
            "adjustments_total"   typed IntegerType,
            "taxes_total"         typed IntegerType,
            "grand_total"         typed IntegerType         
          ),
          // Purchased items
          "purchased_item_count"  typed IntegerType,
          "purchased_items"       nested (
            "sku"   typed StringType analyzer "autocomplete",
            "name"  typed StringType analyzer "autocomplete",
            "price" typed IntegerType
          ),
          // Addresses
          "shipping_addresses"    nested (
            "address1"          typed StringType analyzer "autocomplete",
            "address2"          typed StringType analyzer "autocomplete",
            "city"              typed StringType analyzer "autocomplete",
            "zip"               typed StringType index "not_analyzed",
            "region"            typed StringType analyzer "autocomplete",
            "country"           typed StringType analyzer "autocomplete",
            "continent"         typed StringType analyzer "autocomplete",
            "currency"          typed  StringType analyzer "autocomplete"
          ),
          "billing_addresses"     nested (
            "address1"          typed StringType analyzer "autocomplete",
            "address2"          typed StringType analyzer "autocomplete",
            "city"              typed StringType analyzer "autocomplete",
            "zip"               typed StringType index "not_analyzed",
            "region"            typed StringType analyzer "autocomplete",
            "country"           typed StringType analyzer "autocomplete",
            "continent"         typed StringType analyzer "autocomplete",
            "currency"          typed  StringType analyzer "autocomplete"
          ),
          // Store credits
          "store_credit_total"    typed IntegerType,
          "store_credit_count"    typed IntegerType,
          // Saved for later
          "saved_for_later_count" typed IntegerType,
          "saved_for_later"       nested (
            "sku"     typed StringType analyzer "autocomplete",
            "name"    typed StringType analyzer "autocomplete",
            "price"   typed IntegerType
          )
        ),
        "orders_search_view" as (
          // Order
          "id"                  typed IntegerType,
          "reference_number"    typed StringType analyzer "autocomplete",
          "status"              typed StringType index "not_analyzed",
          "created_at"          typed DateType,
          "placed_at"           typed DateType,
          "currency"            typed StringType index "not_analyzed",
          // Totals
          "sub_total"           typed IntegerType,
          "shipping_total"      typed IntegerType,
          "adjustments_total"   typed IntegerType,
          "taxes_total"         typed IntegerType,
          "grand_total"         typed IntegerType,
          // Customer
          "customer"            nested (
            "name"                  typed StringType analyzer "autocomplete",
            "email"                 typed StringType analyzer "autocomplete",
            "is_blacklisted"        typed BooleanType,
            "joined_at"             typed DateType,
            "revenue"               typed IntegerType,
            "rank"                  typed IntegerType
          ),
          // Line items
          "line_item_cont"      typed IntegerType,
          "line_items"          nested (
            "status"  typed StringType index "not_analyzed",
            "sku"     typed StringType analyzer "autocomplete",
            "name"    typed StringType analyzer "autocomplete",
            "price"   typed IntegerType
          ),
          // Payments
          "payments"            nested (
            "payment_method_type" typed StringType index "not_analyzed",
            "amount"              typed IntegerType,
            "currency"            typed StringType index "not_analyzed"
          ),
          "credit_card_count"   typed IntegerType,
          "credit_card_total"   typed IntegerType,
          "gift_card_count"     typed IntegerType,
          "gift_card_total"     typed IntegerType,
          "store_credit_count"  typed IntegerType,
          "store_credit_total"  typed IntegerType,
          // Shipments
          "shipment_count"      typed IntegerType,
          "shipments"           nested (
            "status"                  typed StringType index "not_analyzed",
            "shipping_price"          typed IntegerType,
            "admin_display_name"      typed StringType analyzer "autocomplete",
            "storefront_display_name" typed StringType analyzer "autocomplete"
          ),
          // Addresses
          "shipping_addresses"  nested (
            "address1"          typed StringType analyzer "autocomplete",
            "address2"          typed StringType analyzer "autocomplete",
            "city"              typed StringType analyzer "autocomplete",
            "zip"               typed StringType index "not_analyzed",
            "region"            typed StringType analyzer "autocomplete",
            "country"           typed StringType analyzer "autocomplete",
            "continent"         typed StringType analyzer "autocomplete",
            "currency"          typed  StringType analyzer "autocomplete"
          ),
          "billing_addresses"   nested (
            "address1"          typed StringType analyzer "autocomplete",
            "address2"          typed StringType analyzer "autocomplete",
            "city"              typed StringType analyzer "autocomplete",
            "zip"               typed StringType index "not_analyzed",
            "region"            typed StringType analyzer "autocomplete",
            "country"           typed StringType analyzer "autocomplete",
            "continent"         typed StringType analyzer "autocomplete",
            "currency"          typed  StringType analyzer "autocomplete"
          ),
          // Assignments
          "assignment_count"    typed IntegerType,
          "assignees"           nested (
            "first_name"    typed StringType analyzer "autocomplete",
            "last_name"     typed StringType analyzer "autocomplete",
            "assigned_at"   typed DateType
          ),
          // RMAs
          "rma_count"           typed IntegerType,
          "rmas"                nested (
            "reference_number"  typed StringType analyzer "autocomplete",
            "status"            typed StringType index "not_analyzed",
            "rma_type"          typed StringType index "not_analyzed",
            "placed_at"         typed DateType
          )
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