package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class TaxonomiesSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "taxonomies_search_view"
  def mapping() = esMapping(topic()).fields(
    field("id", LongType),
    field("taxonomyId", IntegerType),
    field("name", StringType)
      .analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("context", StringType).index("not_analyzed"),
    field("scope", StringType).index("not_analyzed"),
    field("type", StringType).index("not_analyzed"),
    field("valuesCount", IntegerType),
    field("createdAt", DateType).format(dateFormat),
    field("updatedAt", DateType).format(dateFormat),
    field("activeFrom", DateType).format(dateFormat),
    field("activeTo", DateType).format(dateFormat),
    field("archivedAt", DateType).format(dateFormat)
  )
}
