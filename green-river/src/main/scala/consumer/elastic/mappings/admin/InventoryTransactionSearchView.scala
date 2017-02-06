package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class InventoryTransactionSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("inventory_transactions_search_view").fields(
      field("id", IntegerType),
      field("sku", StringType).analyzer("upper_cased"),
      field("type", StringType).index("not_analyzed"),
      field("status", StringType).index("not_analyzed"),
      field("stockLocationName", StringType).analyzer("autocomplete"),
      field("quantityPrevious", IntegerType),
      field("quantityNew", IntegerType),
      field("quantityChange", IntegerType),
      field("afsNew", IntegerType),
      field("createdAt", DateType).format(dateFormat),
      field("scope", StringType).index("not_analyzed"),
      field("skuId", IntegerType)
  )
}
