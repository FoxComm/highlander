package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings._

final case class CartsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("carts_search_view").fields(
      // Cart
      field("id", IntegerType),
      field("scope", StringType).index("not_analyzed"),
      field("referenceNumber", StringType).analyzer("upper_cased"),
      field("createdAt", DateType).format(dateFormat),
      field("updatedAt", DateType).format(dateFormat),
      field("currency", StringType) index "not_analyzed",
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
          field("joinedAt", DateType) format dateFormat,
          field("revenue", IntegerType),
          field("rank", IntegerType)
      ),
      // Line items
      field("lineItemCount", IntegerType),
      field("lineItems").nested(
          field("referenceNumber", StringType).analyzer("upper_cased"),
          field("state", StringType) index "not_analyzed",
          field("sku", StringType) analyzer "upper_cased",
          field("name", StringType) analyzer "autocomplete",
          field("externalId", StringType) index "not_analyzed",
          field("price", IntegerType)
      ),
      // Payments
      field("payments").nested(
          field("paymentMethodType", StringType) index "not_analyzed",
          field("amount", IntegerType),
          field("currency", StringType) index "not_analyzed"
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
          field("state", StringType) index "not_analyzed",
          field("shippingPrice", IntegerType),
          field("adminDisplayName", StringType) analyzer "autocomplete",
          field("storefrontDisplayName", StringType) analyzer "autocomplete"
      ),
      // Addresses
      field("shippingAddressesCount", IntegerType),
      address("shippingAddresses"),
      field("billingAddressesCount", IntegerType),
      address("billingAddresses"),
      // Cart-specific
      field("deletedAt", DateType) format dateFormat
  )

  override def nestedFields() = List(
      "customer",
      "line_items",
      "payments",
      "shipments",
      "shipping_addresses",
      "billing_addresses"
  )
}
