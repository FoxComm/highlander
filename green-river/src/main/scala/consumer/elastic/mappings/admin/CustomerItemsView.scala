package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class CustomerItemsView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("customer_items_view").fields(
      field("id", IntegerType),
      field("scope", StringType).index("not_analyzed"),
      // Customer
      field("customerId", IntegerType),
      field("customerName", StringType).analyzer("autocomplete"),
      field("customerEmail", StringType).analyzer("autocomplete"),
      // SKU
      field("skuCode", StringType).index("not_analyzed"),
      field("skuTitle", StringType).analyzer("autocomplete"),
      field("skuPrice", IntegerType),
      // Order
      field("orderReferenceNumber", StringType).analyzer("upper_cased"),
      field("orderPlacedAt", DateType).format(dateFormat),
      // Save for later
      field("savedForLaterAt", DateType).format(dateFormat)
  )
}
