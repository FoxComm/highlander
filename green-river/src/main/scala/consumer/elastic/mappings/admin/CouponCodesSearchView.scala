package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class CouponCodesSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("coupon_codes_search_view").fields(
      field("id", IntegerType),
      field("code", StringType).analyzer("upper_cased"),
      field("couponId", IntegerType),
      field("promotionId", IntegerType),
      field("scope", StringType).index("not_analyzed"),
      field("totalUsed", IntegerType),
      field("createdAt", DateType).format(dateFormat)
  )
}
