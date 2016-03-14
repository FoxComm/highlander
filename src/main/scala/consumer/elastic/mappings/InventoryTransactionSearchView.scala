package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class InventoryTransactionSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("inventory_transactions_search_view").fields(
    field("id", IntegerType),
    field("createdAt", DateType) format dateFormat,
    field("warehouse", StringType) analyzer "autocomplete",
    field("event", StringType),
    field("previousQuantity", IntegerType),
    field("newQuantity", IntegerType),
    field("change", IntegerType),
    field("newAfs", IntegerType),
    field("skuType", StringType) index "not_analyzed",
    field("state", StringType) index "not_analyzed"
  )
}
