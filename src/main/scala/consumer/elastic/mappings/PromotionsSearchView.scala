package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class PromotionsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("promotions_search_view").fields(
    field("id", IntegerType),
    field("context", StringType) index "not_analyzed",
    field("applyType", StringType) index "not_analyzed",
    field("name", StringType).analyzer("autocomplete"),
    field("storefrontName", StringType).analyzer("autocomplete"),
    field("description", StringType).analyzer("autocomplete"),
    field("activeFrom", DateType) format dateFormat,
    field("activeTo", DateType) format dateFormat,    
    field("totalUsed", IntegerType).analyzer("not_analyzed"),
    field("currentCarts", IntegerType).analyzer("not_analyzed"),
    field("createdAt", DateType) format dateFormat,
    field("discounts", IntegerType).analyzer("not_analyzed")
  )

  override def nestedFields() = List("discounts")
}
