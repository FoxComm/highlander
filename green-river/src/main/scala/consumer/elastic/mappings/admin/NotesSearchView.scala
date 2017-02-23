package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class NotesSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "notes_search_view"
  def mapping() = esMapping(topic()).fields(
      // Note
      field("id", IntegerType),
      field("scope", StringType).index("not_analyzed"),
      field("referenceId", IntegerType),
      field("referenceType", StringType).index("not_analyzed"),
      field("body", StringType).analyzer("autocomplete"),
      field("priority", StringType).index("not_analyzed"),
      field("createdAt", DateType).format(dateFormat),
      field("deletedAt", DateType).format(dateFormat),
      field("author").nested(
          field("email", StringType).analyzer("autocomplete"),
          field("name", StringType).analyzer("autocomplete"),
          field("department", StringType).analyzer("autocomplete")
      ),
      field("order").nested(
          field("accountId", IntegerType),
          field("referenceNumber", StringType).analyzer("upper_cased"),
          field("state", StringType).index("not_analyzed"),
          field("createdAt", DateType).format(dateFormat),
          field("placedAt", DateType).format(dateFormat),
          field("subTotal", IntegerType),
          field("shippingTotal", IntegerType),
          field("adjustmentsTotal", IntegerType),
          field("taxesTotal", IntegerType),
          field("grandTotal", IntegerType),
          field("itemsCount", IntegerType)
      ),
      field("customer").nested(
          field("id", IntegerType),
          field("name", StringType).analyzer("autocomplete"),
          field("email", StringType).analyzer("autocomplete"),
          field("isBlacklisted", BooleanType),
          field("joinedAt", DateType).format(dateFormat)
      ),
      field("giftCard").nested(
          field("code", StringType).analyzer("upper_cased"),
          field("originType", StringType).index("not_analyzed"),
          field("currency", StringType).index("not_analyzed"),
          field("createdAt", DateType).format(dateFormat)
      ),
      field("skuItem").nested(
          field("id", IntegerType),
          field("sku", StringType).analyzer("upper_cased"),
          field("type", ObjectType),
          field("attributes", ObjectType),
          field("createdAt", DateType).format(dateFormat)
      ),
      field("product").nested(
          field("id", IntegerType),
          field("attributes", ObjectType),
          field("variants", ObjectType),
          field("createdAt", DateType).format(dateFormat)
      ),
      field("promotion").nested(
          field("id", IntegerType),
          field("applyType", StringType).index("not_analyzed"),
          field("attributes", ObjectType),
          field("createdAt", DateType).format(dateFormat)
      ),
      field("coupon").nested(
          field("id", IntegerType),
          field("promotion_id", IntegerType),
          field("attributes", ObjectType),
          field("createdAt", DateType).format(dateFormat)
      )
  )

  override def nestedFields() =
    List("author", "order", "customer", "gift_card", "product", "sku_item", "promotion", "coupon")
}
