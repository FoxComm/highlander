package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class FailedAuthorizationsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("failed_authorizations_search_view").fields(
    // Credit Card Charge
    field("id", IntegerType),
    field("chargeId", StringType) analyzer "autocomplete",
    field("amount", IntegerType),
    field("currency", StringType) index "not_analyzed",
    field("state", StringType) index "not_analyzed",
    field("createdAt", DateType) format dateFormat,
    // Credit Card
    field("holderName", StringType) analyzer "autocomplete",
    field("lastFour", IntegerType),
    field("expMonth", IntegerType),
    field("expYear", IntegerType),
    field("brand", StringType) analyzer "autocomplete",
    // Billing Address
    field("address1", StringType) analyzer "autocomplete",
    field("address2", StringType) analyzer "autocomplete",
    field("city", StringType) analyzer "autocomplete",
    field("zip", StringType) index "not_analyzed",
    field("region", StringType) analyzer "autocomplete",
    field("country", StringType) analyzer "autocomplete",
    field("continent", StringType) analyzer "autocomplete",
    // Order and Customer
    field("orderReferenceNumber", StringType) index "not_analyzed",
    field("customerId", IntegerType)
  )
}
