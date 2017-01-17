package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings._

final case class InventorySearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("inventory_search_view").fields(
    field("id", IntegerType),
    field("sku", StringType).analyzer("upper_cased"),
    field("stockItem").nested(
      field("id", IntegerType),
      field("sku", StringType).analyzer("upper_cased"),
      field("defaultUnitCost", IntegerType)
    ),
    field("stockLocation").nested(
      field("id", IntegerType),
      field("name", StringType).analyzer("autocomplete"),
      field("type", StringType).index("not_analyzed")
    ),
    field("type", StringType).index("not_analyzed"),
    field("onHand", IntegerType),
    field("onHold", IntegerType),
    field("reserved", IntegerType),
    field("shipped", IntegerType),
    field("afs", IntegerType),
    field("afsCost", IntegerType),
    field("createdAt", DateType).format(dateFormat),
    field("updatedAt", DateType).format(dateFormat),
    field("deletedAt", DateType).format(dateFormat),
    field("scope", StringType).index("not_analyzed")
  )

  override def nestedFields() = List("stock_item", "stock_location")
}
