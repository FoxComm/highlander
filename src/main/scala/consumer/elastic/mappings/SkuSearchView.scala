package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class SkuSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("sku_search_view").fields(
    field("id", IntegerType),
    field("context", StringType) index "not_analyzed",
    field("code", StringType) index "not_analyzed",
    field("title", StringType).analyzer("autocomplete"),
    field("is_hazardous", BooleanType),
    field("price", IntegerType)
  )
}
