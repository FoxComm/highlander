package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class InventorySearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("inventory_search_view").fields(
      field("id", IntegerType),
      field("product", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("productActive", BooleanType),
      field("sku", StringType).analyzer("upper_cased"),
      field("skuActive", BooleanType),
      field("skuType", StringType).analyzer("autocomplete"),
      field("warehouse", StringType).analyzer("autocomplete"),
      field("onHand", IntegerType),
      field("onHold", IntegerType),
      field("reserved", IntegerType),
      field("safetyStock", IntegerType),
      field("afs", IntegerType)
  )
}
