package consumer.elastic

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.AvroJsonHelper

abstract class AvroTransformer(implicit ec: ExecutionContext) extends JsonTransformer {

  def fields(): List[String]

  def transform(json: String): Future[String] = Future {
    AvroJsonHelper.transformJson(json, fields)
  }
}

object AvroTransformers {
  val dateFormat = "yyyy-MM-dd HH:mm:ss"
  val strictDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  def address(name: String) =
    name nested (
      "address1"  typed StringType analyzer "autocomplete",
      "address2"  typed StringType analyzer "autocomplete",
      "city"      typed StringType analyzer "autocomplete",
      "zip"       typed StringType index "not_analyzed",
      "region"    typed StringType analyzer "autocomplete",
      "country"   typed StringType analyzer "autocomplete",
      "continent" typed StringType analyzer "autocomplete",
      "currency"  typed StringType analyzer "autocomplete"
    )

  final case class Sku()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "skus" as (
        "id"            typed IntegerType,
        "sku"           typed StringType analyzer "autocomplete",
        "name"          typed StringType analyzer "autocomplete",
        "is_hazardous"  typed BooleanType,
        "price"         typed IntegerType
      )

    def fields = List.empty
  }

  final case class GiftCardsSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "gift_cards_search_view" as (
        "id"                   typed IntegerType,
        "code"                 typed StringType analyzer "autocomplete",
        "originId"             typed IntegerType,
        "originType"           typed StringType index "not_analyzed",
        "subtype"              typed StringType analyzer "autocomplete",
        "status"               typed StringType index "not_analyzed",
        "currency"             typed StringType index "not_analyzed",
        "originalBalance"      typed IntegerType,
        "currentBalance"       typed IntegerType,
        "availableBalance"     typed IntegerType,
        "canceledAmount"       typed IntegerType,
        "canceledReason" nested (
          "reasonType"        typed StringType analyzer "autocomplete",
          "body"               typed StringType analyzer "autocomplete"
        ),
        "createdAt"            typed DateType format dateFormat,
        "updatedAt"            typed DateType format dateFormat,
        "storeAdmin" nested (
          "email"              typed StringType analyzer "autocomplete",
          "name"               typed StringType analyzer "autocomplete",
          "department"         typed StringType analyzer "autocomplete"
        ),
        "storeCredit" nested (
          "id"                 typed IntegerType,
          "customerId"         typed IntegerType,
          "originType"         typed StringType index "not_analyzed",
          "currency"           typed StringType index "not_analyzed",
          "status"             typed StringType index "not_analyzed"
        )
      )

    def fields = List("store_admin", "store_credit", "canceled_reason")
  }

  final case class StoreCreditsSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "store_credits_search_view" as (
        "id"                   typed IntegerType,
        "customerId"           typed IntegerType,
        "originId"             typed IntegerType,
        "originType"           typed StringType index "not_analyzed",
        "subtype"              typed StringType analyzer "autocomplete",
        "status"               typed StringType index "not_analyzed",
        "currency"             typed StringType index "not_analyzed",
        "originalBalance"      typed IntegerType,
        "currentBalance"       typed IntegerType,
        "availableBalance"     typed IntegerType,
        "canceledAmount"       typed IntegerType,
        "canceledReason" nested (
          "reasonType"        typed StringType analyzer "autocomplete",
          "body"               typed StringType analyzer "autocomplete"
        ),
        "createdAt"            typed DateType format dateFormat,
        "updatedAt"            typed DateType format dateFormat,
        "storeAdmin" nested (
          "email"              typed StringType analyzer "autocomplete",
          "name"               typed StringType analyzer "autocomplete",
          "department"         typed StringType analyzer "autocomplete"
        ),
        "giftCard" nested (
          "code"               typed StringType analyzer "autocomplete",
          "originType"         typed StringType index "not_analyzed",
          "currency"           typed StringType index "not_analyzed",
          "status"             typed StringType index "not_analyzed"
        )
      )

    def fields = List("store_admin", "gift_card", "canceled_reason")
  }

  final case class CustomersSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "customers_search_view" as (
        // Customer
        "id"                      typed IntegerType,
        "name"                    typed StringType analyzer "autocomplete",
        "email"                   typed StringType analyzer "autocomplete",
        "isDisabled"              typed BooleanType,
        "isGuest"                 typed BooleanType,
        "isBlacklisted"           typed BooleanType,
        "phoneNumber"             typed StringType index "not_analyzed",
        "location"                typed StringType analyzer "autocomplete",
        "joinedAt"                typed DateType format dateFormat,
        "revenue"                 typed IntegerType,
        "rank"                    typed IntegerType,
        // Orders
        "orderCount"              typed IntegerType,
        "orders" nested(
          "customerId"            typed IntegerType,
          "referenceNumber"       typed StringType analyzer "autocomplete",
          "state"                 typed StringType index "not_analyzed",
          "createdAt"             typed DateType format dateFormat,
          "placedAt"              typed DateType format dateFormat,
          "subTotal"              typed IntegerType,
          "shippingTotal"         typed IntegerType,
          "adjustmentsTotal"      typed IntegerType,
          "taxesTotal"            typed IntegerType,
          "grandTotal"            typed IntegerType
        ),
        // Purchased items
        "purchasedItemCount"      typed IntegerType,
        "purchasedItems" nested (
          "sku"                   typed StringType analyzer "autocomplete",
          "name"                  typed StringType analyzer "autocomplete",
          "price"                 typed IntegerType
        ),
        // Addresses
        "shippingAddressesCount"  typed IntegerType,
        address("shippingAddresses"),
        "billingAddressesCount"   typed IntegerType,
        address("billingAddresses"),
        // Store credits
        "storeCreditTotal"        typed IntegerType,
        "storeCreditCount"        typed IntegerType,
        // Saved for later
        "savedForLaterCount"      typed IntegerType,
        "savedForLater" nested (
          "sku"                   typed StringType analyzer "autocomplete",
          "name"                  typed StringType analyzer "autocomplete",
          "price"                 typed IntegerType
        )
      )

    def fields = List("orders", "purchased_items", "shipping_addresses", "billing_addresses", "save_for_later")
  }

  final case class OrdersSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping = {
      "orders_search_view" as (
        // Order
        "id"                      typed IntegerType,
        "referenceNumber"         typed StringType analyzer "autocomplete",
        "state"                   typed StringType index "not_analyzed",
        "createdAt"               typed DateType format dateFormat,
        "placedAt"                typed DateType format dateFormat,
        "currency"                typed StringType index "not_analyzed",
        // Totals
        "subTotal"                typed IntegerType,
        "shippingTotal"           typed IntegerType,
        "adjustmentsTotal"        typed IntegerType,
        "taxesTotal"              typed IntegerType,
        "grandTotal"              typed IntegerType,
        // Customer
        "customer" nested (
          "id"                    typed IntegerType,
          "name"                  typed StringType analyzer "autocomplete",
          "email"                 typed StringType analyzer "autocomplete",
          "isBlacklisted"         typed BooleanType,
          "joinedAt"              typed DateType format dateFormat,
          "revenue"               typed IntegerType,
          "rank"                  typed IntegerType
        ),
        // Line items
        "lineItemCount"           typed IntegerType,
        "lineItems" nested (
          "status"                typed StringType index "not_analyzed",
          "sku"                   typed StringType analyzer "autocomplete",
          "name"                  typed StringType analyzer "autocomplete",
          "price"                 typed IntegerType
        ),
        // Payments
        "payments" nested (
          "paymentMethodType"     typed StringType index "not_analyzed",
          "amount"                typed IntegerType,
          "currency"              typed StringType index "not_analyzed"
        ),
        "creditCardCount"         typed IntegerType,
        "creditCardTotal"         typed IntegerType,
        "giftCardCount"           typed IntegerType,
        "giftCardTotal"           typed IntegerType,
        "storeCreditCount"        typed IntegerType,
        "storeCreditTotal"        typed IntegerType,
        // Shipments
        "shipmentCount"           typed IntegerType,
        "shipments" nested (
          "status"                typed StringType index "not_analyzed",
          "shippingPrice"         typed IntegerType,
          "adminDisplayName"      typed StringType analyzer "autocomplete",
          "storefrontDisplayName" typed StringType analyzer "autocomplete"
        ),
        // Addresses
        "shippingAddressesCount"  typed IntegerType,
        address("shippingAddresses"),
        "billingAddressesCount"   typed IntegerType,
        address("billingAddresses"),
        // Assignments
        "assignmentCount"         typed IntegerType,
        "assignees" nested (
          "name"                  typed StringType analyzer "autocomplete",
          "assignedAt"            typed DateType format dateFormat
        ),
        // RMAs
        "rmaCount"                typed IntegerType,
        "rmas" nested (
          "referenceNumber"       typed StringType analyzer "autocomplete",
          "status"                typed StringType index "not_analyzed",
          "rmaType"               typed StringType index "not_analyzed",
          "placedAt"              typed DateType format dateFormat
        )
      )
    }

    def fields = List("customer", "orders", "line_items", "payments", "shipments", "shipping_addresses",
      "billing_addresses", "assignees", "rmas"
    )
  }

  final case class StoreAdminsSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "store_admins_search_view" as (
        // Store Admin
        "id"                typed IntegerType,
        "email"             typed StringType analyzer "autocomplete",
        "name"              typed StringType analyzer "autocomplete",
        "department"        typed StringType analyzer "autocomplete",
        "createdAt"         typed DateType format dateFormat,
        // Assignments
        "assignmentsCount"  typed IntegerType,
        "assignments" nested (
          "referenceNumber" typed StringType analyzer "autocomplete",
          "assignedAt"      typed DateType format dateFormat
        )
      )

    def fields = List("assignments")
  }

  final case class FailedAuthorizationsSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "failed_authorizations_search_view" as (
        // Credit Card Charge
        "id"                    typed IntegerType,
        "chargeId"              typed StringType analyzer "autocomplete",
        "amount"                typed IntegerType,
        "currency"              typed StringType index "not_analyzed",
        "status"                typed StringType index "not_analyzed",
        "createdAt"             typed DateType format dateFormat,
        // Credit Card
        "holderName"            typed StringType analyzer "autocomplete",
        "lastFour"              typed IntegerType,
        "expMonth"              typed IntegerType,
        "expYear"               typed IntegerType,
        "brand"                 typed StringType analyzer "autocomplete",
        // Billing Address
        "address1"              typed StringType analyzer "autocomplete",
        "address2"              typed StringType analyzer "autocomplete",
        "city"                  typed StringType analyzer "autocomplete",
        "zip"                   typed StringType index "not_analyzed",
        "region"                typed StringType analyzer "autocomplete",
        "country"               typed StringType analyzer "autocomplete",
        "continent"             typed StringType analyzer "autocomplete",
        // Order and Customer
        "orderReferenceNumber"  typed StringType analyzer "autocomplete",
        "customerId"            typed IntegerType
      )

    def fields = List.empty
  }

  final case class NotesSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "notes_search_view" as (
        // Note
        "id"                    typed IntegerType,
        "referenceType"         typed StringType index "not_analyzed",
        "body"                  typed StringType analyzer "autocomplete",
        "priority"              typed StringType index "not_analyzed",
        "createdAt"             typed DateType format dateFormat,
        "deletedAt"             typed DateType format dateFormat,
        "storeAdmin" nested (
          "email"       typed StringType analyzer "autocomplete",
          "name"        typed StringType analyzer "autocomplete",
          "department"  typed StringType analyzer "autocomplete"
        ),
        "order" nested(
          "customerId"            typed IntegerType,
          "referenceNumber"       typed StringType analyzer "autocomplete",
          "state"                 typed StringType index "not_analyzed",
          "createdAt"             typed DateType format dateFormat,
          "placedAt"              typed DateType format dateFormat,
          "subTotal"              typed IntegerType,
          "shippingTotal"         typed IntegerType,
          "adjustmentsTotal"      typed IntegerType,
          "taxesTotal"            typed IntegerType,
          "grandTotal"            typed IntegerType
        ),
        "customer" nested (
          "id"                    typed IntegerType,
          "name"                  typed StringType analyzer "autocomplete",
          "email"                 typed StringType analyzer "autocomplete",
          "isBlacklisted"         typed BooleanType,
          "joinedAt"              typed DateType format dateFormat
        ),
        "giftCard" nested (
          "code"                  typed StringType analyzer "autocomplete",
          "originType"            typed StringType index "not_analyzed",
          "currency"              typed StringType index "not_analyzed",
          "createdAt"             typed DateType format dateFormat
        )
      )

    def fields = List("store_admin", "order", "customer", "gift_card")
  }

  final case class StoreCreditTransactionsView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "store_credit_transactions_view" as (
        // Adjustment
        "id"                    typed IntegerType,
        "debit"                 typed IntegerType,
        "availableBalance"      typed IntegerType,
        "status"                typed StringType index "not_analyzed",
        "createdAt"             typed DateType format dateFormat,
        // Store Credit
        "storeCreditId"         typed IntegerType,
        "customerId"            typed IntegerType,
        "originType"            typed StringType index "not_analyzed",
        "currency"              typed StringType index "not_analyzed",
        "storeCreditCreatedAt"  typed DateType format dateFormat,
        // Order
        "orderReferenceNumber"  typed StringType analyzer "autocomplete",
        "orderCreatedAt"        typed DateType format dateFormat,
        "orderPaymentCreatedAt" typed DateType format dateFormat,
        // Store Admins
        "storeAdmin" nested (
          "email"       typed StringType analyzer "autocomplete",
          "name"        typed StringType analyzer "autocomplete",
          "department"  typed StringType analyzer "autocomplete"
        )
      )

    def fields = List("store_admin")
  }

  final case class GiftCardTransactionsView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "gift_card_transactions_view" as (
        // Adjustment
        "id"                    typed IntegerType,
        "debit"                 typed IntegerType,
        "credit"                typed IntegerType,
        "availableBalance"      typed IntegerType,
        "status"                typed StringType index "not_analyzed",
        "createdAt"             typed DateType format dateFormat,
        // Gift Card
        "code"                  typed StringType analyzer "autocomplete",
        "originType"            typed StringType index "not_analyzed",
        "currency"              typed StringType index "not_analyzed",
        "giftCardCreatedAt"     typed DateType format dateFormat,
        // Order
        "orderReferenceNumber"  typed StringType analyzer "autocomplete",
        "orderCreatedAt"        typed DateType format dateFormat,
        "orderPaymentCreatedAt" typed DateType format dateFormat,
        // Store Admins
        "storeAdmin" nested (
          "email"       typed StringType analyzer "autocomplete",
          "name"        typed StringType analyzer "autocomplete",
          "department"  typed StringType analyzer "autocomplete"
        )
      )

    def fields = List("store_admin")
  }
}
