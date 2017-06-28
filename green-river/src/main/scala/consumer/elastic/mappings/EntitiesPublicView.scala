package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer

final case class EntitiesPublicView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "geronimo_entities"
  def mapping() = esMapping(topic()).fields(
    field("id", LongType),
    field("kind", StringType).index("not_analyzed"),
    field("content", StringType).index("not_analyzed"),
    field("storefront", StringType).index("not_analyzed")
  )
}
