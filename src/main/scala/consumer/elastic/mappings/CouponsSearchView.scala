package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class CouponsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("coupons_search_view").fields(
    field("id", IntegerType),
    field("couponId", IntegerType),
    field("promotionId", IntegerType),
    field("context", StringType) index "not_analyzed",
    field("name", StringType).analyzer("autocomplete"),
    field("storefrontName", StringType).index("not_analyzed"),
    field("description", StringType).analyzer("autocomplete"),
    field("activeFrom", DateType) format dateFormat,
    field("activeTo", DateType) format dateFormat,
    field("totalUsed", IntegerType),
    field("createdAt", DateType) format dateFormat
  )
}
