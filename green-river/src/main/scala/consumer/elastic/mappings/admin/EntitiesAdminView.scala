package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class EntitiesAdminView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "geronimo_entities"
  def mapping() = esMapping(topic()).fields(
    field("id", LongType),
    field("kind", StringType).index("not_analyzed"),
    field("content", StringType).index("not_analyzed"),
    field("storefront", StringType).index("not_analyzed"),
    field("schema_version", DateType).format(dateFormat),
    field("content_type_id", IntegerType),
    field("created_by", IntegerType),
    field("scope", StringType).index("not_analyzed"),
    field("inserted_at", DateType).format(dateFormat),
    field("updated_at", DateType).format(dateFormat)
  )
}
