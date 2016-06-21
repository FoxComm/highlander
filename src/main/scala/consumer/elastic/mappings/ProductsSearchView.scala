package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class ProductsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("products_search_view").fields(
      field("id", IntegerType),
      field("productId", IntegerType),
      field("context", StringType) index "not_analyzed",
      field("title", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("description", StringType).analyzer("autocomplete"),
      field("skus", StringType) index "not_analyzed",
      field("tags", StringType) index "not_analyzed",
      field("activeFrom", DateType) format dateFormat,
      field("activeTo", DateType) format dateFormat,
      field("albums").nested(
          field("name", StringType) index "not_analyzed",
          field("images").nested(
              field("alt", StringType) index "not_analyzed",
              field("src", StringType) index "not_analyzed",
              field("title", StringType) index "not_analyzed"
          )
      )
  )

  override def nestedFields() = List("images", "skus", "tags")
}
