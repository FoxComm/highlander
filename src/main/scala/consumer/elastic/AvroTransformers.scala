package consumer.elastic

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.AvroJsonHelper

abstract class AvroTransformer extends JsonTransformer { 

  def fields() : AvroJsonHelper.FieldMap

  def transform(json: String) : String = {
        AvroJsonHelper.transformJson(json, fields)
  }
}

object AvroTransformers { 

  def address(name: String) = 
    name nested (
      "address1"          typed StringType analyzer "autocomplete",
      "address2"          typed StringType analyzer "autocomplete",
      "city"              typed StringType analyzer "autocomplete",
      "zip"               typed StringType index "not_analyzed",
      "region"            typed StringType analyzer "autocomplete",
      "country"           typed StringType analyzer "autocomplete",
      "continent"         typed StringType analyzer "autocomplete",
      "currency"          typed  StringType analyzer "autocomplete"
    )


  final case class Customer() extends AvroTransformer { 
    def mapping = "customers" as ()

    def fields = Map(
      "orders" → "orders", 
      "purchased_items" → "purchasedItems", 
      "shipping_addresses" → "shippingAddresses", 
      "billing_addresses" → "billingAddresses",
      "save_for_later" → "saveForLater"
    )
  }

  final case class Order() extends AvroTransformer { 
    def mapping = "orders" as ()

    def fields = Map(
      "customer" → "customer", 
      "line_items" → "lineItems", 
      "payments" → "payments", 
      "shipments" → "shipments", 
      "shipping_addresses" → "shippingAddresses",
      "billing_addresses" → "billingAddresses", 
      "assignees" → "assignees", 
      "rmas" → "rmas"
    )

  }

  final case class Country() extends AvroTransformer { 
    def mapping = 
      "countries" as (
        "id"        typed IntegerType,
        "name"      typed StringType analyzer "autocomplete",
        "continent" typed StringType index "not_analyzed",
        "currency"  typed StringType index "not_analyzed"
      )

    def fields = Map.empty

  }

  final case class Region() extends AvroTransformer { 
    def mapping = 
      "regions" as (
        "id"        typed IntegerType,
        "name"      typed StringType analyzer "autocomplete",
        "continent" typed StringType index "not_analyzed",
        "currency"  typed StringType index "not_analyzed"
      )

    def fields = Map.empty
  }

  final case class CustomerSearchView() extends AvroTransformer { 
    def mapping = 
      "customers_search_view" as (
        // Customer
        "id"                    typed IntegerType,
        "name"                  typed StringType analyzer "autocomplete",
        "email"                 typed StringType analyzer "autocomplete",
        "isDisabled"           typed BooleanType,
        "isGuest"              typed BooleanType,
        "isBlacklisted"        typed BooleanType,
        "joinedAt"             typed DateType,
        "revenue"               typed IntegerType,
        "rank"                  typed IntegerType,
        // Orders
        "orderCount"           typed IntegerType,
        "orders"                nested (
          "referenceNumber"    typed StringType analyzer "autocomplete",
          "status"              typed StringType index "not_analyzed",
          "createdAt"          typed DateType,
          "placedAt"           typed DateType,
          "subTotal"           typed IntegerType,
          "shippingTotal"      typed IntegerType,
          "adjustmentsTotal"   typed IntegerType,
          "taxesTotal"         typed IntegerType,
          "grandTotal"         typed IntegerType
        ),
        // Purchased items
        "purchasedItemCount"  typed IntegerType,
        "purchasedItems"       nested (
          "sku"   typed StringType analyzer "autocomplete",
          "name"  typed StringType analyzer "autocomplete",
          "price" typed IntegerType
        ),
        // Addresses
        "shippingAddressesCount" typed IntegerType,
        address("shippingAddresses"),
        "billingAddressesCount" typed IntegerType,
        address("billingAddresses"),
        // Store credits
        "storeCreditTotal"    typed IntegerType,
        "storeCreditCount"    typed IntegerType,
        // Saved for later
        "savedForLaterCount" typed IntegerType,
        "savedForLater"       nested (
          "sku"     typed StringType analyzer "autocomplete",
          "name"    typed StringType analyzer "autocomplete",
          "price"   typed IntegerType
        )
      )

    def fields = Map.empty

  }

  final case class OrderSearchView() extends AvroTransformer { 
    def mapping = {
      "orders_search_view" as (
        // Order
        "id"                  typed IntegerType,
        "referenceNumber"    typed StringType analyzer "autocomplete",
        "status"              typed StringType index "not_analyzed",
        "createdAt"          typed DateType,
        "placedAt"           typed DateType,
        "currency"            typed StringType index "not_analyzed",
        // Totals
        "subTotal"           typed IntegerType,
        "shippingTotal"      typed IntegerType,
        "adjustmentsTotal"   typed IntegerType,
        "taxesTotal"         typed IntegerType,
        "grandTotal"         typed IntegerType,
        // Customer
        "customer"            nested (
          "name"                  typed StringType analyzer "autocomplete",
          "email"                 typed StringType analyzer "autocomplete",
          "isBlacklisted"        typed BooleanType,
          "joinedAt"             typed DateType,
          "revenue"               typed IntegerType,
          "rank"                  typed IntegerType
        ),
        // Line items
        "lineItemCount"      typed IntegerType,
        "lineItems"          nested (
          "status"  typed StringType index "not_analyzed",
          "sku"     typed StringType analyzer "autocomplete",
          "name"    typed StringType analyzer "autocomplete",
          "price"   typed IntegerType
        ),
        // Payments
        "payments"            nested (
          "paymentMethodType" typed StringType index "not_analyzed",
          "amount"              typed IntegerType,
          "currency"            typed StringType index "not_analyzed"
        ),
        "creditCardCount"   typed IntegerType,
        "creditCardTotal"   typed IntegerType,
        "giftCardCount"     typed IntegerType,
        "giftCardTotal"     typed IntegerType,
        "storeCreditCount"  typed IntegerType,
        "storeCreditTotal"  typed IntegerType,
        // Shipments
        "shipmentCount"      typed IntegerType,
        "shipments"           nested (
          "status"                  typed StringType index "not_analyzed",
          "shippingPrice"          typed IntegerType,
          "adminDisplayName"      typed StringType analyzer "autocomplete",
          "storefrontDisplayName" typed StringType analyzer "autocomplete"
        ),
        // Addresses
        "shippingAddressesCount" typed IntegerType,
        address("shippingAddresses"),
        "billingAddressesCount" typed IntegerType,
        address("billingAddresses"),
        // Assignments
        "assignmentCount"    typed IntegerType,
        "assignees"           nested (
          "firstName"    typed StringType analyzer "autocomplete",
          "lastName"     typed StringType analyzer "autocomplete",
          "assignedAt"   typed DateType
        ),
        // RMAs
        "rmaCount"           typed IntegerType,
        "rmas"                nested (
          "referenceNumber"  typed StringType analyzer "autocomplete",
          "status"            typed StringType index "not_analyzed",
          "rmaType"          typed StringType index "not_analyzed",
          "placedAt"         typed DateType
        )
      )
    }

    def fields = Map.empty
  }
}
