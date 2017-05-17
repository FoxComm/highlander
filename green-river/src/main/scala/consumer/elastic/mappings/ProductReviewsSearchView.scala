package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class ProductReviewsSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "product_reviews_search_view"
  def mapping() = esMapping(topic()).fields(
      field("id", LongType),
      field("sku", StringType).index("not_analyzed"),
      field("userName", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("title", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("body", StringType).analyzer("autocomplete"),
      field("createdAt", DateType).format(dateFormat),
      field("updatedAt", DateType).format(dateFormat),
      field("archivedAt", DateType).format(dateFormat)
  )
}
