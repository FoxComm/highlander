package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings._

final case class OrdersSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("orders_search_view").fields(
      // Order
      field("id", IntegerType),
      field("referenceNumber", StringType).analyzer("upper_cased"),
      field("state", StringType).index("not_analyzed"),
      field("scope", StringType).index("not_analyzed"),
      field("createdAt", DateType).format(dateFormat),
      field("placedAt", DateType).format(dateFormat),
      field("currency", StringType).index("not_analyzed"),
      // Totals
      field("subTotal", IntegerType),
      field("shippingTotal", IntegerType),
      field("adjustmentsTotal", IntegerType),
      field("taxesTotal", IntegerType),
      field("grandTotal", IntegerType),
      // Customer
      field("customer").nested(
          field("id", IntegerType),
          field("name", StringType)
            .analyzer("autocomplete")
            .fields(field("raw", StringType).index("not_analyzed")),
          field("email", StringType)
            .analyzer("autocomplete")
            .fields(field("raw", StringType).index("not_analyzed")),
          field("isBlacklisted", BooleanType),
          field("joinedAt", DateType).format(dateFormat),
          field("revenue", IntegerType),
          field("rank", IntegerType)
      ),
      // Line items
      field("lineItemCount", IntegerType),
      field("lineItems").nested(
          field("referenceNumber", StringType).analyzer("upper_cased"),
          field("state", StringType).index("not_analyzed"),
          field("sku", StringType).index("not_analyzed"),
          field("name", StringType).analyzer("autocomplete"),
          field("price", IntegerType)
      ),
      // Payments
      field("payments").nested(
          field("paymentMethodType", StringType).index("not_analyzed"),
          field("amount", IntegerType),
          field("currency", StringType).index("not_analyzed")
      ),
      field("creditCardCount", IntegerType),
      field("creditCardTotal", IntegerType),
      field("giftCardCount", IntegerType),
      field("giftCardTotal", IntegerType),
      field("storeCreditCount", IntegerType),
      field("storeCreditTotal", IntegerType),
      // Shipments
      field("shipmentCount", IntegerType),
      field("shipments").nested(
          field("state", StringType).index("not_analyzed"),
          field("shippingPrice", IntegerType),
          field("adminDisplayName", StringType).analyzer("autocomplete"),
          field("storefrontDisplayName", StringType).analyzer("autocomplete")
      ),
      field("shippingMethod").nested(
          field("id", IntegerType),
          field("price", IntegerType),
          field("shippingMethodId", IntegerType),
          field("adminDisplayName", StringType).analyzer("autocomplete"),
          field("storefrontDisplayName", StringType).analyzer("autocomplete")
      ),
      // Addresses
      field("shippingAddressesCount", IntegerType),
      address("shippingAddresses"),
      field("billingAddressesCount", IntegerType),
      address("billingAddresses"),
      // Assignments
      field("assignmentCount", IntegerType),
      field("assignees").nested(
          field("name", StringType).analyzer("autocomplete"),
          field("assignedAt", DateType).format(dateFormat)
      ),
      // Returns
      field("returns").nested(
          field("referenceNumber", StringType).analyzer("upper_cased"),
          field("state", StringType).index("not_analyzed"),
          field("returnType", StringType).index("not_analyzed"),
          field("placedAt", DateType).format(dateFormat)
      )
  )

  override def nestedFields() = List(
      "customer",
      "line_items",
      "payments",
      "shipments",
      "shipping_method",
      "shipping_addresses",
      "billing_addresses",
      "assignees",
      "returns"
  )
}
