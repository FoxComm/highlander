package consumer.elastic.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.MappingHelpers._

final case class CustomersSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("customers_search_view").fields(
    // Customer
    field("id", IntegerType),
    field("name", StringType).analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("email", StringType).analyzer("autocomplete")
      .fields(field("raw", StringType).index("not_analyzed")),
    field("isDisabled", BooleanType),
    field("isGuest", BooleanType),
    field("isBlacklisted", BooleanType),
    field("phoneNumber", StringType) index "not_analyzed",
    field("location", StringType) analyzer "autocomplete",
    field("joinedAt", DateType) format dateFormat,
    field("revenue", IntegerType),
    field("rank", IntegerType),
    // Orders
    field("orderCount", IntegerType),
    field("orders").nested(
      field("customerId", IntegerType),
      field("referenceNumber", StringType) index "not_analyzed",
      field("state", StringType) index "not_analyzed",
      field("createdAt", DateType) format dateFormat,
      field("placedAt", DateType) format dateFormat,
      field("subTotal", IntegerType),
      field("shippingTotal", IntegerType),
      field("adjustmentsTotal", IntegerType),
      field("taxesTotal", IntegerType),
      field("grandTotal", IntegerType)
    ),
    // Addresses
    field("shippingAddressesCount", IntegerType),
    address("shippingAddresses"),
    field("billingAddressesCount", IntegerType),
    address("billingAddresses"),
    // Store credits
    field("storeCreditTotal", IntegerType),
    field("storeCreditCount", IntegerType)
  )

  override def nestedFields() = List(
    "orders",
    "shipping_addresses",
    "billing_addresses"
  )
}
