package consumer.elastic.mappings


import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class GiftCardsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("gift_cards_search_view").fields(
    field("sku_id", IntegerType),
    field("code", StringType) index "not_analyzed",
    field("context", StringType) index "not_analyzed",
    field("originId", IntegerType),
    field("originType", StringType) index "not_analyzed",
    field("subtype", StringType) analyzer "autocomplete",
    field("state", StringType) index "not_analyzed",
    field("currency", StringType) index "not_analyzed",
    field("originalBalance", IntegerType),
    field("currentBalance", IntegerType),
    field("availableBalance", IntegerType),
    field("canceledAmount", IntegerType),
    field("canceledReason").nested (
      field("reasonType", StringType) analyzer "autocomplete",
      field("body", StringType) analyzer "autocomplete"
    ),
    field("createdAt", DateType) format dateFormat,
    field("updatedAt", DateType) format dateFormat,
    field("storeAdmin").nested (
      field("email", StringType) analyzer "autocomplete",
      field("name", StringType) analyzer "autocomplete",
      field("department", StringType) analyzer "autocomplete"
    ),
    field("storeCredit").nested (
      field("id", IntegerType),
      field("customerId", IntegerType),
      field("originType", StringType) index "not_analyzed",
      field("currency", StringType) index "not_analyzed",
      field("state", StringType) index "not_analyzed"
    )
  )

  override def nestedFields() = List("store_admin", "store_credit", "canceled_reason")
}
