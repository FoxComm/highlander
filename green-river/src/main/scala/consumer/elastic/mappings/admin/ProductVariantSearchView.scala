package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class ProductVariantSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("product_variant_search_view").fields(
      field("id", IntegerType),
      field("skuCode", StringType).analyzer("upper_cased"),
      field("context", StringType).index("not_analyzed"),
      field("scope", StringType).index("not_analyzed"),
      field("title", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("salePrice", IntegerType),
      field("salePriceCurrency", StringType),
      field("retailPrice", IntegerType),
      field("retailPriceCurrency", StringType),
      field("archivedAt", DateType).format(dateFormat),
      field("externalId", StringType).index("not_analyzed")
  )
}
