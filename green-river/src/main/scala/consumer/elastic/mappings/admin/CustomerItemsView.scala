package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class CustomerItemsView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "customer_items_view"
  def mapping() = esMapping(topic()).fields(
      field("id", LongType),
      field("scope", StringType).index("not_analyzed"),
      // Customer
      field("customerId", IntegerType),
      field("customerName", StringType).analyzer("autocomplete"),
      field("customerEmail", StringType).analyzer("autocomplete"),
      // SKU
      field("skuCode", StringType).analyzer("upper_cased"),
      field("skuTitle", StringType).analyzer("autocomplete"),
      field("skuPrice", IntegerType),
      // Order
      field("orderReferenceNumber", StringType).analyzer("upper_cased"),
      field("orderPlacedAt", DateType).format(dateFormat),
      field("lineItemState", StringType).index("not_analyzed"),
      // Save for later
      field("savedForLaterAt", DateType).format(dateFormat)
  )
}
