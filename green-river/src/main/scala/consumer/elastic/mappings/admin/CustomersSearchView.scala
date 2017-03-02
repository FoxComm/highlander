package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings._
import consumer.elastic.mappings.dateFormat

final case class CustomersSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "customers_search_view"
  def mapping() = esMapping(topic()).fields(
      // Customer
      field("id", LongType),
      field("scope", StringType).index("not_analyzed"),
      field("name", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("email", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("isDisabled", BooleanType),
      field("isGuest", BooleanType),
      field("isBlacklisted", BooleanType),
      field("blacklistedBy", IntegerType),
      field("blacklistedReason", StringType).analyzer("autocomplete"),
      field("phoneNumber", StringType).index("not_analyzed"),
      field("location", StringType).analyzer("autocomplete"),
      field("joinedAt", DateType).format(dateFormat),
      field("revenue", IntegerType),
      field("rank", IntegerType),
      // Orders
      field("orderCount", IntegerType),
      field("orders").nested(
          field("customerId", IntegerType),
          field("referenceNumber", StringType).analyzer("upper_cased"),
          field("state", StringType).index("not_analyzed"),
          field("createdAt", DateType).format(dateFormat),
          field("placedAt", DateType).format(dateFormat),
          field("subTotal", IntegerType),
          field("shippingTotal", IntegerType),
          field("adjustmentsTotal", IntegerType),
          field("taxesTotal", IntegerType),
          field("grandTotal", IntegerType),
          field("itemsCount", IntegerType)
      ),
      // Addresses
      field("shippingAddressesCount", IntegerType),
      address("shippingAddresses"),
      field("billingAddressesCount", IntegerType),
      address("billingAddresses"),
      // Store credits
      field("storeCreditTotal", IntegerType),
      field("storeCreditCount", IntegerType),
      // Revenue and rank
      field("revenue", IntegerType),
      field("rank", IntegerType)
  )

  override def nestedFields() = List(
      "orders",
      "shipping_addresses",
      "billing_addresses"
  )
}
