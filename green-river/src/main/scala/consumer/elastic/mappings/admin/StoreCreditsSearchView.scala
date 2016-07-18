package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class StoreCreditsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("store_credits_search_view").fields(
      field("id", IntegerType),
      field("customerId", IntegerType),
      field("originId", IntegerType),
      field("originType", StringType) index "not_analyzed",
      field("subtype", StringType) analyzer "autocomplete",
      field("state", StringType) index "not_analyzed",
      field("currency", StringType) index "not_analyzed",
      field("originalBalance", IntegerType),
      field("currentBalance", IntegerType),
      field("availableBalance", IntegerType),
      field("canceledAmount", IntegerType),
      field("canceledReason").nested(
          field("reasonType", StringType) analyzer "autocomplete",
          field("body", StringType) analyzer "autocomplete"
      ),
      field("createdAt", DateType) format dateFormat,
      field("updatedAt", DateType) format dateFormat,
      field("storeAdmin").nested(
          field("email", StringType) analyzer "autocomplete",
          field("name", StringType) analyzer "autocomplete",
          field("department", StringType) analyzer "autocomplete"
      ),
      field("giftCard").nested(
          field("code", StringType) index "not_analyzed",
          field("originType", StringType) index "not_analyzed",
          field("currency", StringType) index "not_analyzed",
          field("state", StringType) index "not_analyzed"
      ),
      field("metadata").nested()
  )

  override def nestedFields() = List("store_admin", "gift_card", "canceled_reason", "metadata")
}
