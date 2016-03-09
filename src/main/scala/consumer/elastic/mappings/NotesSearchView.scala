package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class NotesSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("notes_search_view").fields(
    // Note
    field("id", IntegerType),
    field("referenceType", StringType) index "not_analyzed",
    field("body", StringType) analyzer "autocomplete",
    field("priority", StringType) index "not_analyzed",
    field("createdAt", DateType) format dateFormat,
    field("deletedAt", DateType) format dateFormat,
    field("author").nested (
      field("email", StringType) analyzer "autocomplete",
      field("name", StringType) analyzer "autocomplete",
      field("department", StringType) analyzer "autocomplete"
    ),
    field("order").nested(
      field("customerId", IntegerType),
      field("referenceNumber", StringType) index "not_analyzed",
      field("state", StringType) index "not_analyzed",
      field("createdAt", DateType) format dateFormat,
      field("placedAt", DateType) format dateFormat,
      field("subTotal", IntegerType),
      field("shippingTotal", IntegerType),
      field("adjustmentsTotal", IntegerType),
      field("taxesTotal", IntegerType),
      field("grandTotal", IntegerType)
    ),
    field("customer").nested (
      field("id", IntegerType),
      field("name", StringType) analyzer "autocomplete",
      field("email", StringType) analyzer "autocomplete",
      field("isBlacklisted", BooleanType),
      field("joinedAt", DateType) format dateFormat
    ),
    field("giftCard").nested (
      field("code", StringType) index "not_analyzed",
      field("originType", StringType) index "not_analyzed",
      field("currency", StringType) index "not_analyzed",
      field("createdAt", DateType) format dateFormat
    )
  )

  override def nestedFields() = List("author", "order", "customer", "gift_card")
}
