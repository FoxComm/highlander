package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

case class CustomerGroupsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("customer_groups_search_view").fields(
      field("id", IntegerType),
      field("groupId", IntegerType),
      field("name", StringType).analyzer("autocomplete"),
      field("customersCount", IntegerType),
      field("clientState", StringType).index("not_analyzed"),
      field("elasticRequest", StringType).index("not_analyzed"),
      field("scope", StringType).index("not_analyzed"),
      field("createdAt", DateType).format(dateFormat),
      field("updatedAt", DateType).format(dateFormat)
  )
}
