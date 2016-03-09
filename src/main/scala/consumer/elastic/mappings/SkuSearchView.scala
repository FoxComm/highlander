package consumer.elastic.mappings

import scala.concurrent.ExecutionContext

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.elastic.AvroTransformer

final case class SkuSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
  def mapping() = esMapping("sku_search_view").fields(
    field("id", IntegerType),
    field("context", StringType) index "not_analyzed",
    field("code", StringType) index "not_analyzed",
    field("title", StringType).analyzer("autocomplete"),
    field("is_hazardous", BooleanType),
    field("price", IntegerType)
  )
}