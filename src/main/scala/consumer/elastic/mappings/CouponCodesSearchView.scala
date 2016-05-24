package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class CouponCodesSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("coupon_codes_search_view").fields(
      field("id", IntegerType),
      field("code", StringType) index "not_analyzed",
      field("couponId", IntegerType),
      field("promotionId", IntegerType),
      field("totalUsed", IntegerType),
      field("createdAt", DateType) format dateFormat
  )
}
