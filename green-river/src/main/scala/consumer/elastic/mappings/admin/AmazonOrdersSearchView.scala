package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class AmazonOrdersSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "amazon_orders_search_view"
  def mapping() = esMapping(topic()).fields(
      field("id", LongType),
      field("amazonOrderId", StringType).index("not_analyzed"),
      field("orderTotal", IntegerType),
      field("paymentMethodDetail", StringType).index("not_analyzed"),
      field("orderType", StringType).index("not_analyzed"),
      field("currency", StringType).index("not_analyzed"),
      field("orderStatus", StringType).index("not_analyzed"),
      field("purchaseDate", DateType).format(dateFormat),
      field("createdAt", DateType).format(dateFormat)
      field("updatedAt", DateType).format(dateFormat),
  )
}
