package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class ProductsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("products_search_view").fields(
      field("id", IntegerType),
      field("productId", IntegerType),
      field("slug", StringType).index("not_analyzed"),
      field("context", StringType).index("not_analyzed"),
      field("scope", StringType).index("not_analyzed"),
      field("title", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("description", StringType).analyzer("autocomplete"),
      field("skus", StringType).analyzer("upper_cased"),
      field("tags", StringType).index("not_analyzed"),
      field("activeFrom", DateType).format(dateFormat),
      field("activeTo", DateType).format(dateFormat),
      field("archivedAt", DateType).format(dateFormat),
      field("externalId", StringType).index("not_analyzed"),
      field("albums").nested(
          field("name", StringType).index("not_analyzed"),
          field("archivedAt", DateType).format(dateFormat),
          field("images").nested(
              field("alt", StringType).index("not_analyzed"),
              field("src", StringType).index("not_analyzed"),
              field("title", StringType).index("not_analyzed"),
              field("baseUrl", StringType).index("not_analyzed")
          )
      )
  )

  override def nestedFields() = List("albums", "skus", "tags")
}
