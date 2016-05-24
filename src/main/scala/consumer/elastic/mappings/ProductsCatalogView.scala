package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class ProductsCatalogView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("products_catalog_view").fields(
      field("id", IntegerType),
      field("context", StringType) index "not_analyzed",
      field("title", StringType).analyzer("autocomplete"),
      field("images", StringType) index "not_analyzed",
      field("description", StringType).analyzer("autocomplete"),
      field("salePrice", IntegerType).analyzer("autocomplete"),
      field("tags", StringType) index "not_analyzed"
  )

  override def nestedFields() = List("images", "tags")
}
