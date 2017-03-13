package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class TaxonsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("taxons_search_view").fields(
      field("id", IntegerType),
      field("taxonomyId", IntegerType),
      field("taxonId", IntegerType),
      field("parentId", IntegerType),
      field("name", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("context", StringType).index("not_analyzed"),
      field("scope", StringType).index("not_analyzed"),
      field("createdAt", DateType).format(dateFormat),
      field("updatedAt", DateType).format(dateFormat),
      field("activeFrom", DateType).format(dateFormat),
      field("activeTo", DateType).format(dateFormat),
      field("archivedAt", DateType).format(dateFormat)
  )
}
