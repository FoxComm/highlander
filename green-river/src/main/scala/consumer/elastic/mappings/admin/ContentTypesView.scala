package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class ContentTypesView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "geronimo_content_types"
  def mapping() = esMapping(topic()).fields(
    field("id", LongType),
    field("name", StringType).index("not_analyzed"),
    field("schema", StringType).index("not_analyzed"),
    field("scope", StringType).index("not_analyzed"),
    field("created_by", IntegerType),
    field("inserted_at", DateType).format(dateFormat),
    field("updated_at", DateType).format(dateFormat),
    field("scope", StringType).index("not_analyzed")
  )
}
