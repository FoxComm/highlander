package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class SkuSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("sku_search_view").fields(
      field("id", IntegerType),
      field("code", StringType).analyzer("upper_cased"),
      field("context", StringType).index("not_analyzed"),
      field("title", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("price", IntegerType),
      field("archivedAt", DateType).format(dateFormat)
  )
}
