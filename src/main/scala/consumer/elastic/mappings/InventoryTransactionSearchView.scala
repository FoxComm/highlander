package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class InventoryTransactionSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("inventory_transactions_search_view").fields(
      field("id", IntegerType),
      field("sku", StringType) index "not_analyzed",
      field("createdAt", DateType) format dateFormat,
      field("warehouse", StringType) analyzer "autocomplete",
      field("event", StringType),
      field("previousQuantity", IntegerType),
      field("newQuantity", IntegerType),
      field("change", IntegerType),
      field("newAfs", IntegerType),
      field("skuType", StringType) analyzer "autocomplete",
      field("state", StringType) index "not_analyzed"
  )
}
