package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class RegionsSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "regions_search_view"
  def mapping() = esMapping(topic()).fields(
      field("id", IntegerType),
      field("name", StringType).analyzer("autocomplete"),
      field("abbreviation", StringType).index("not_analyzed"),
      field("countryId", IntegerType)
  )
}
