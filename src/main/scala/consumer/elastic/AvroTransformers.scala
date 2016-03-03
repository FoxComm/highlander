package consumer.elastic

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping}
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.AvroJsonHelper

abstract class AvroTransformer(implicit ec: ExecutionContext) extends JsonTransformer {

  def nestedFields(): List[String]

  def transform(json: String): Future[String] = Future {
    AvroJsonHelper.transformJson(json, nestedFields())
  }
}

object AvroTransformers {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  def address(name: String) = field(name).nested(
      field("address1", StringType).analyzer("autocomplete"),
      field("address2", StringType).analyzer("autocomplete"),
      field("city", StringType).analyzer("autocomplete"),
      field("zip", StringType).index("not_analyzed"),
      field("region", StringType).analyzer("autocomplete"),
      field("country", StringType).analyzer("autocomplete"),
      field("continent", StringType).analyzer("autocomplete"),
      field("currency", StringType).analyzer("autocomplete")
    )

  final case class SkuSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() = esMapping("sku_search_view").fields(
        field("id", IntegerType),
        field("context", StringType) index "not_analyzed",
        field("code", StringType) index "not_analyzed",
        field("title", StringType).analyzer("autocomplete"),
        field("is_hazardous", BooleanType),
        field("price", IntegerType)
      )

    def nestedFields() = List.empty
  }

  final case class GiftCardsSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("gift_cards_search_view").fields(
        field("id", IntegerType),
        field("code", StringType) index "not_analyzed",
        field("originId", IntegerType),
        field("originType", StringType) index "not_analyzed",
        field("subtype", StringType) analyzer "autocomplete",
        field("state", StringType) index "not_analyzed",
        field("currency", StringType) index "not_analyzed",
        field("originalBalance", IntegerType),
        field("currentBalance", IntegerType),
        field("availableBalance", IntegerType),
        field("canceledAmount", IntegerType),
        field("canceledReason").nested (
          field("reasonType", StringType) analyzer "autocomplete",
          field("body", StringType) analyzer "autocomplete"
        ),
        field("createdAt", DateType) format dateFormat,
        field("updatedAt", DateType) format dateFormat,
        field("storeAdmin").nested (
          field("email", StringType) analyzer "autocomplete",
          field("name", StringType) analyzer "autocomplete",
          field("department", StringType) analyzer "autocomplete"
        ),
        field("storeCredit").nested (
          field("id", IntegerType),
          field("customerId", IntegerType),
          field("originType", StringType) index "not_analyzed",
          field("currency", StringType) index "not_analyzed",
          field("state", StringType) index "not_analyzed"
        )
      )

    def nestedFields() = List("store_admin", "store_credit", "canceled_reason")
  }

  final case class StoreCreditsSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("store_credits_search_view").fields(
        field("id", IntegerType),
        field("customerId", IntegerType),
        field("originId", IntegerType),
        field("originType", StringType) index "not_analyzed",
        field("subtype", StringType) analyzer "autocomplete",
        field("state", StringType) index "not_analyzed",
        field("currency", StringType) index "not_analyzed",
        field("originalBalance", IntegerType),
        field("currentBalance", IntegerType),
        field("availableBalance", IntegerType),
        field("canceledAmount", IntegerType),
        field("canceledReason").nested (
          field("reasonType", StringType) analyzer "autocomplete",
          field("body", StringType) analyzer "autocomplete"
        ),
        field("createdAt", DateType) format dateFormat,
        field("updatedAt", DateType) format dateFormat,
        field("storeAdmin").nested (
          field("email", StringType) analyzer "autocomplete",
          field("name", StringType) analyzer "autocomplete",
          field("department", StringType) analyzer "autocomplete"
        ),
        field("giftCard").nested (
          field("code", StringType) index "not_analyzed",
          field("originType", StringType) index "not_analyzed",
          field("currency", StringType) index "not_analyzed",
          field("state", StringType) index "not_analyzed"
        )
      )

    def nestedFields() = List("store_admin", "gift_card", "canceled_reason")
  }

  final case class CustomersSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("customers_search_view").fields(
        // Customer
        field("id", IntegerType),
        field("name", StringType) analyzer "autocomplete",
        field("email", StringType) analyzer "autocomplete",
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
        // Purchased items
        field("purchasedItemCount", IntegerType),
        field("purchasedItems").nested (
          field("sku", StringType) index "not_analyzed",
          field("name", StringType) analyzer "autocomplete",
          field("price", IntegerType)
        ),
        // Addresses
        field("shippingAddressesCount", IntegerType),
        address("shippingAddresses"),
        field("billingAddressesCount", IntegerType),
        address("billingAddresses"),
        // Store credits
        field("storeCreditTotal", IntegerType),
        field("storeCreditCount", IntegerType),
        // Saved for later
        field("savedForLaterCount", IntegerType),
        field("savedForLater").nested (
          field("sku", StringType) index "not_analyzed",
          field("name", StringType) analyzer "autocomplete",
          field("price", IntegerType)
        )
      )

    def nestedFields() = List("orders", "purchased_items", "shipping_addresses", "billing_addresses", "save_for_later")
  }

  final case class OrdersSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() = {
      esMapping("orders_search_view").fields(
        // Order
        field("id", IntegerType),
        field("referenceNumber", StringType) index "not_analyzed",
        field("state", StringType) index "not_analyzed",
        field("createdAt", DateType) format dateFormat,
        field("placedAt", DateType) format dateFormat,
        field("currency", StringType) index "not_analyzed",
        // Totals
        field("subTotal", IntegerType),
        field("shippingTotal", IntegerType),
        field("adjustmentsTotal", IntegerType),
        field("taxesTotal", IntegerType),
        field("grandTotal", IntegerType),
        // Customer
        field("customer").nested (
          field("id", IntegerType),
          field("name", StringType) analyzer "autocomplete",
          field("email", StringType) analyzer "autocomplete",
          field("isBlacklisted", BooleanType),
          field("joinedAt", DateType) format dateFormat,
          field("revenue", IntegerType),
          field("rank", IntegerType)
        ),
        // Line items
        field("lineItemCount", IntegerType),
        field("lineItems").nested (
          field("referenceNumber", StringType) index "not_analyzed",
          field("state", StringType) index "not_analyzed",
          field("sku", StringType) index "not_analyzed",
          field("name", StringType) analyzer "autocomplete",
          field("price", IntegerType)
        ),
        // Payments
        field("payments").nested (
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
        field("shipments").nested (
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
        // Assignments
        field("assignmentCount", IntegerType),
        field("assignees").nested (
          field("name", StringType) analyzer "autocomplete",
          field("assignedAt", DateType) format dateFormat
        ),
        // RMAs
        field("rmaCount", IntegerType),
        field("rmas").nested (
          field("referenceNumber", StringType) index "not_analyzed",
          field("state", StringType) index "not_analyzed",
          field("rmaType", StringType) index "not_analyzed",
          field("placedAt", DateType) format dateFormat
        )
      )
    }

    def nestedFields() = List("customer", "orders", "line_items", "payments", "shipments", "shipping_addresses",
      "billing_addresses", "assignees", "rmas"
    )
  }

  final case class StoreAdminsSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("store_admins_search_view").fields(
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

    def nestedFields() = List("assignments")
  }

  final case class FailedAuthorizationsSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("failed_authorizations_search_view").fields(
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

    def nestedFields() = List.empty
  }

  final case class NotesSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("notes_search_view").fields(
        // Note
        field("id", IntegerType),
        field("referenceType", StringType) index "not_analyzed",
        field("body", StringType) analyzer "autocomplete",
        field("priority", StringType) index "not_analyzed",
        field("createdAt", DateType) format dateFormat,
        field("deletedAt", DateType) format dateFormat,
        field("author").nested (
          field("email", StringType) analyzer "autocomplete",
          field("name", StringType) analyzer "autocomplete",
          field("department", StringType) analyzer "autocomplete"
        ),
        field("order").nested(
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
        field("customer").nested (
          field("id", IntegerType),
          field("name", StringType) analyzer "autocomplete",
          field("email", StringType) analyzer "autocomplete",
          field("isBlacklisted", BooleanType),
          field("joinedAt", DateType) format dateFormat
        ),
        field("giftCard").nested (
          field("code", StringType) index "not_analyzed",
          field("originType", StringType) index "not_analyzed",
          field("currency", StringType) index "not_analyzed",
          field("createdAt", DateType) format dateFormat
        )
      )

    def nestedFields() = List("author", "order", "customer", "gift_card")
  }

  final case class StoreCreditTransactionsView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("store_credit_transactions_view").fields(
        // Adjustment
        field("id", IntegerType),
        field("debit", IntegerType),
        field("availableBalance", IntegerType),
        field("state", StringType) index "not_analyzed",
        field("createdAt", DateType) format dateFormat,
        // Store Credit
        field("storeCreditId", IntegerType),
        field("customerId", IntegerType),
        field("originType", StringType) index "not_analyzed",
        field("currency", StringType) index "not_analyzed",
        field("storeCreditCreatedAt", DateType) format dateFormat,
        // Order Payment
        field("orderPayment").nested (
          field("orderReferenceNumber", StringType) index "not_analyzed",
          field("orderCreatedAt", DateType) format dateFormat,
          field("orderPaymentCreatedAt", DateType) format dateFormat
        ),
        // Store Admins
        field("storeAdmin").nested (
          field("email", StringType) analyzer "autocomplete",
          field("name", StringType) analyzer "autocomplete",
          field("department", StringType) analyzer "autocomplete"
        )
      )

    def nestedFields() = List("store_admin", "order_payment")
  }

  final case class GiftCardTransactionsView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("gift_card_transactions_view").fields(
        // Adjustment
        field("id", IntegerType),
        field("debit", IntegerType),
        field("credit", IntegerType),
        field("availableBalance", IntegerType),
        field("state", StringType) index "not_analyzed",
        field("createdAt", DateType) format dateFormat,
        // Gift Card
        field("code", StringType) index "not_analyzed",
        field("originType", StringType) index "not_analyzed",
        field("currency", StringType) index "not_analyzed",
        field("giftCardCreatedAt", DateType) format dateFormat,
        // Order Payment
        field("orderPayment").nested (
          field("orderReferenceNumber", StringType) index "not_analyzed",
          field("orderCreatedAt", DateType) format dateFormat,
          field("orderPaymentCreatedAt", DateType) format dateFormat
        ),
        // Store Admins
        field("storeAdmin").nested (
          field("email", StringType) analyzer "autocomplete",
          field("name", StringType) analyzer "autocomplete",
          field("department", StringType) analyzer "autocomplete"
        )
      )

    def nestedFields() = List("store_admin", "order_payment")
  }

  final case class InventorySearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping() =
      esMapping("inventory_search_view").fields(
        field("id", IntegerType),
        field("product", StringType) analyzer "autocomplete",
        field("productActive", BooleanType),
        field("sku", StringType) index "not_analyzed",
        field("skuActive", BooleanType),
        field("skuType", StringType) analyzer "autocomplete",
        field("warehouse", StringType) analyzer "autocomplete",
        field("onHand", IntegerType),
        field("onHold", IntegerType),
        field("reserved", IntegerType),
        field("safetyStock", IntegerType),
        field("afs", IntegerType)
      )

    def nestedFields() = List.empty
  }

}
