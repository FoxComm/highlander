package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class CountriesSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("countries_search_view").fields(
    field("id", IntegerType),
    field("name", StringType).analyzer("autocomplete"),
    field("alpha2", StringType).index("not_analyzed"),
    field("alpha3", StringType).index("not_analyzed"),
    field("code", StringType).index("not_analyzed"),
    field("continent", StringType).analyzer("autocomplete"),
    field("currency", StringType).index("not_analyzed"),
    field("uses_postal_code", BooleanType),
    field("is_billable", BooleanType),
    field("is_shippable", BooleanType)
  )
}
