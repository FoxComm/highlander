package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class CouponsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("coupons_search_view").fields(
      field("id", IntegerType),
      field("promotionId", IntegerType),
      field("context", StringType).index("not_analyzed"),
      field("name", StringType).analyzer("autocomplete"),
      field("storefrontName", StringType).analyzer("autocomplete"),
      field("description", StringType).analyzer("autocomplete"),
      field("activeFrom", DateType).format(dateFormat),
      field("activeTo", DateType).format(dateFormat),
      field("totalUsed", IntegerType),
      field("createdAt", DateType).format(dateFormat),
      field("archivedAt", DateType).format(dateFormat)
  )
}
