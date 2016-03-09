package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class StoreAdminsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("store_admins_search_view").fields(
    // Store Admin
    field("id", IntegerType),
    field("email", StringType) analyzer "autocomplete",
    field("name", StringType) analyzer "autocomplete",
    field("department", StringType) analyzer "autocomplete",
    field("createdAt", DateType) format dateFormat,
    // Assignments
    field("assignmentsCount", IntegerType),
    field("assignments").nested (
      field("referenceNumber", StringType) index "not_analyzed",
      field("assignedAt", DateType) format dateFormat
    )
  )

  override def nestedFields() = List("assignments")
}
