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

  def address(name: String) =
    name nested (
      "address1" typed StringType analyzer "autocomplete",
      "address2" typed StringType analyzer "autocomplete",
      "city" typed StringType analyzer "autocomplete",
      "zip" typed StringType index "not_analyzed",
      "region" typed StringType analyzer "autocomplete",
      "country" typed StringType analyzer "autocomplete",
      "continent" typed StringType analyzer "autocomplete",
      "currency" typed StringType analyzer "autocomplete"
    )

  final case class Country()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "countries" as (
        "id" typed IntegerType,
        "name" typed StringType analyzer "autocomplete",
        "continent" typed StringType index "not_analyzed",
        "currency" typed StringType index "not_analyzed"
      )

    def fields = List.empty

  }

  final case class Region()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "regions" as (
        "id" typed IntegerType,
        "name" typed StringType analyzer "autocomplete",
        "continent" typed StringType index "not_analyzed",
        "currency" typed StringType index "not_analyzed"
      )

    def fields = List.empty
  }

  final case class CustomersSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping =
      "customers_search_view" as (
        // Customer
        "id" typed IntegerType,
        "name" typed StringType analyzer "autocomplete",
        "email" typed StringType analyzer "autocomplete",
        "isDisabled" typed BooleanType,
        "isGuest" typed BooleanType,
        "isBlacklisted" typed BooleanType,
        "joinedAt" typed DateType format "yyyy-MM-dd HH:mm:ss",
        "revenue" typed IntegerType,
        "rank" typed IntegerType,
        // Orders
        "orderCount" typed IntegerType,
        "orders" nested(
          "referenceNumber" typed StringType analyzer "autocomplete",
          "status" typed StringType index "not_analyzed",
          "createdAt" typed DateType format "yyyy-MM-dd HH:mm:ss",
          "placedAt" typed DateType format "yyyy-MM-dd HH:mm:ss",
          "subTotal" typed IntegerType,
          "shippingTotal" typed IntegerType,
          "adjustmentsTotal" typed IntegerType,
          "taxesTotal" typed IntegerType,
          "grandTotal" typed IntegerType
        ),
        // Purchased items
        "purchasedItemCount" typed IntegerType,
        "purchasedItems" nested (
          "sku" typed StringType analyzer "autocomplete",
          "name" typed StringType analyzer "autocomplete",
          "price" typed IntegerType
        ),
        // Addresses
        "shippingAddressesCount" typed IntegerType,
        address("shippingAddresses"),
        "billingAddressesCount" typed IntegerType,
        address("billingAddresses"),
        // Store credits
        "storeCreditTotal" typed IntegerType,
        "storeCreditCount" typed IntegerType,
        // Saved for later
        "savedForLaterCount" typed IntegerType,
        "savedForLater" nested(
          "sku" typed StringType analyzer "autocomplete",
          "name" typed StringType analyzer "autocomplete",
          "price" typed IntegerType
        )
      )

    def fields = List("orders", "purchased_items", "shipping_addresses", "billing_addresses", "save_for_later")
  }

  final case class OrdersSearchView()(implicit ec: ExecutionContext) extends AvroTransformer {
    def mapping = {
      "orders_search_view" as (
        // Order
        "id" typed IntegerType,
        "referenceNumber" typed StringType analyzer "autocomplete",
        "status" typed StringType index "not_analyzed",
        "createdAt" typed DateType format "yyyy-MM-dd HH:mm:ss",
        "placedAt" typed DateType format "yyyy-MM-dd HH:mm:ss",
        "currency" typed StringType index "not_analyzed",
        // Totals
        "subTotal" typed IntegerType,
        "shippingTotal" typed IntegerType,
        "adjustmentsTotal" typed IntegerType,
        "taxesTotal" typed IntegerType,
        "grandTotal" typed IntegerType,
        // Customer
        "customer" nested (
          "name" typed StringType analyzer "autocomplete",
          "email" typed StringType analyzer "autocomplete",
          "isBlacklisted" typed BooleanType,
          "joinedAt" typed DateType format "yyyy-MM-dd HH:mm:ss",
          "revenue" typed IntegerType,
          "rank" typed IntegerType
        ),
        // Line items
        "lineItemCount" typed IntegerType,
        "lineItems" nested (
          "status" typed StringType index "not_analyzed",
          "sku" typed StringType analyzer "autocomplete",
          "name" typed StringType analyzer "autocomplete",
          "price" typed IntegerType
        ),
        // Payments
        "payments" nested (
          "paymentMethodType" typed StringType index "not_analyzed",
          "amount" typed IntegerType,
          "currency" typed StringType index "not_analyzed"
        ),
        "creditCardCount" typed IntegerType,
        "creditCardTotal" typed IntegerType,
        "giftCardCount" typed IntegerType,
        "giftCardTotal" typed IntegerType,
        "storeCreditCount" typed IntegerType,
        "storeCreditTotal" typed IntegerType,
        // Shipments
        "shipmentCount" typed IntegerType,
        "shipments" nested (
          "status" typed StringType index "not_analyzed",
          "shippingPrice" typed IntegerType,
          "adminDisplayName" typed StringType analyzer "autocomplete",
          "storefrontDisplayName" typed StringType analyzer "autocomplete"
        ),
        // Addresses
        "shippingAddressesCount" typed IntegerType,
        address("shippingAddresses"),
        "billingAddressesCount" typed IntegerType,
        address("billingAddresses"),
        // Assignments
        "assignmentCount" typed IntegerType,
        "assignees" nested(
          "firstName" typed StringType analyzer "autocomplete",
          "lastName" typed StringType analyzer "autocomplete",
          "assignedAt" typed DateType format "yyyy-MM-dd HH:mm:ss"
        ),
        // RMAs
        "rmaCount" typed IntegerType,
        "rmas" nested (
          "referenceNumber" typed StringType analyzer "autocomplete",
          "status" typed StringType index "not_analyzed",
          "rmaType" typed StringType index "not_analyzed",
          "placedAt" typed DateType format "yyyy-MM-dd HH:mm:ss"
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
        // Customer
        "id" typed IntegerType,
        "email" typed StringType analyzer "autocomplete",
        "firstName" typed StringType analyzer "autocomplete",
        "lastName" typed StringType analyzer "autocomplete",
        "department" typed StringType analyzer "autocomplete",
        "createdAt" typed DateType format "yyyy-MM-dd HH:mm:ss",
        // Assignments
        "assignmentsCount" typed IntegerType,
        "assignments" nested(
          "referenceNumber" typed StringType analyzer "autocomplete",
          "assignedAt" typed DateType format "yyyy-MM-dd HH:mm:ss"
        )
      )

    def fields = List("assignments")
  }

}
