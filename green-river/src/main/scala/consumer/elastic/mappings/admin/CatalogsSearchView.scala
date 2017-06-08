package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings._

final case class CatalogsSeachView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "catalogs_search_view"
  def mapping() = esMapping(topic()).fields(
    field("id", LongType),
    field("scope", StringType).index("not_analyzed"),
    field("name", StringType)
      .analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("site", StringType)
      .analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("countryId", IntegerType),
    field("countryName", StringType)
      .analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("defaultLanguage", StringType)
      .analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("createdAt", DateType).format(dateFormat),
    field("updatedAt", DateType).format(dateFormat)
  )
}
