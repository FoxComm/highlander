package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class StoreAdminsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("store_admins_search_view").fields(
    // Store Admin
    field("id", IntegerType),
    field("scope", StringType).index("not_analyzed"),
    field("email", StringType).analyzer("autocomplete"),
    field("name", StringType).analyzer("autocomplete"),
    field("phoneNumber", StringType).index("not_analyzed"),
    field("department", StringType).analyzer("autocomplete"),
    field("state", StringType).index("not_analyzed"),
    field("createdAt", DateType).format(dateFormat)
  )

  override def nestedFields() = List("assignments")
}
