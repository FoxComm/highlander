package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class CustomerItemsView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("customer_items_view").fields(
      field("id", IntegerType),
      // Customer
      field("customerId", IntegerType),
      field("customerName", StringType) analyzer "autocomplete",
      field("customerEmail", StringType) analyzer "autocomplete",
      // SKU
      field("skuCode", BooleanType) index "not_analyzed",
      field("skuTitle", StringType).analyzer("autocomplete"),
      field("skuPrice", IntegerType),
      // Order
      field("orderReferenceNumber", StringType) index "not_analyzed",
      field("orderPlacedAt", DateType) format dateFormat,
      // Save for later
      field("savedForLaterAt", DateType) format dateFormat
  )
}
