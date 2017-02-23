package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings._

final case class PromotionsSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "promotions_search_view"
  def mapping() = esMapping(topic()).fields(
      field("id", IntegerType),
      field("context", StringType).index("not_analyzed"),
      field("scope", StringType).index("not_analyzed"),
      field("applyType", StringType).index("not_analyzed"),
      field("promotionName", StringType).analyzer("autocomplete"),
      field("storefrontName", StringType).analyzer("autocomplete"),
      field("description", StringType).analyzer("autocomplete"),
      field("activeFrom", DateType).format(dateFormat),
      field("activeTo", DateType).format(dateFormat),
      field("totalUsed", IntegerType),
      field("currentCarts", IntegerType),
      field("createdAt", DateType).format(dateFormat),
      field("discounts", IntegerType),
      field("archivedAt", DateType).format(dateFormat)
  )

  override def nestedFields() = List("discounts")
}
