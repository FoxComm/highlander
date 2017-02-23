package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class ProductsCatalogView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "products_catalog_view"
  def mapping() = esMapping(topic()).fields(
      field("id", IntegerType),
      field("slug", StringType).index("not_analyzed"),
      field("context", StringType).index("not_analyzed"),
      field("title", StringType).analyzer("autocomplete"),
      field("description", StringType).analyzer("autocomplete"),
      field("salePrice", IntegerType).analyzer("autocomplete"),
      field("retailPrice", IntegerType).analyzer("autocomplete"),
      field("tags", StringType).index("not_analyzed"),
      field("archivedAt", DateType).format(dateFormat),
      field("skus", StringType).analyzer("upper_cased"),
      field("albums").nested(
          field("name", StringType).index("not_analyzed"),
          field("images").nested(
              field("alt", StringType).index("not_analyzed"),
              field("src", StringType).index("not_analyzed"),
              field("title", StringType).index("not_analyzed"),
              field("baseUrl", StringType).index("not_analyzed")
          )
      )
  )

  override def nestedFields() = List("albums", "tags", "skus")
}
