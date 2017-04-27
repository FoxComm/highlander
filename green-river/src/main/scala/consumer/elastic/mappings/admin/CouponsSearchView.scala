package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class CouponsSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "coupons_search_view"
  def mapping() = esMapping(topic()).fields(
      field("id", LongType),
      field("promotionId", IntegerType),
      field("context", StringType).index("not_analyzed"),
      field("name", StringType).analyzer("autocomplete"),
      field("codes", StringType).index("not_analyzed"),
      field("storefrontName", StringType).analyzer("autocomplete"),
      field("description", StringType).analyzer("autocomplete"),
      field("activeFrom", DateType).format(dateFormat),
      field("activeTo", DateType).format(dateFormat),
      field("totalUsed", IntegerType),
      field("createdAt", DateType).format(dateFormat),
      field("archivedAt", DateType).format(dateFormat)
  )

  override def nestedFields() = List("codes")
}
