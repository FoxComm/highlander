package consumer

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

object ElasticSearchMappings {
  val customerJsonFields = List("orders", "purchased_items", "shipping_addresses", "billing_addresses",
    "save_for_later")

  val orderJsonFields = List("customer", "line_items", "payments", "shipments", "shipping_addresses",
    "billing_addresses", "assignees", "rmas")

  def countries = {
    "countries" as (
      "id"        typed IntegerType,
      "name"      typed StringType analyzer "autocomplete",
      "continent" typed StringType index "not_analyzed",
      "currency"  typed StringType index "not_analyzed"
    )
  }

  def regions = {
    "regions" as (
      "id"          typed IntegerType,
      "country_id"  typed IntegerType,
      "name"        typed StringType analyzer "autocomplete"
    )
  }

  def addresses(name: String) = {
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
  }

  def customers = {
    "customers_search_view" as (
      // Customer
      "id"                    typed IntegerType,
      "name"                  typed StringType analyzer "autocomplete",
      "email"                 typed StringType analyzer "autocomplete",
      "is_disabled"           typed BooleanType,
      "is_guest"              typed BooleanType,
      "is_blacklisted"        typed BooleanType,
      "joined_at"             typed DateType,
      "revenue"               typed IntegerType,
      "rank"                  typed IntegerType,
      // Orders
      "order_count"           typed IntegerType,
      "orders"                nested (
        "reference_number"    typed StringType analyzer "autocomplete",
        "status"              typed StringType index "not_analyzed",
        "created_at"          typed DateType,
        "placed_at"           typed DateType,
        "sub_total"           typed IntegerType,
        "shipping_total"      typed IntegerType,
        "adjustments_total"   typed IntegerType,
        "taxes_total"         typed IntegerType,
        "grand_total"         typed IntegerType
      ),
      // Purchased items
      "purchased_item_count"  typed IntegerType,
      "purchased_items"       nested (
        "sku"   typed StringType analyzer "autocomplete",
        "name"  typed StringType analyzer "autocomplete",
        "price" typed IntegerType
      ),
      // Addresses
      addresses("shipping_addresses"),
      addresses("billing_addresses"),
      // Store credits
      "store_credit_total"    typed IntegerType,
      "store_credit_count"    typed IntegerType,
      // Saved for later
      "saved_for_later_count" typed IntegerType,
      "saved_for_later"       nested (
        "sku"     typed StringType analyzer "autocomplete",
        "name"    typed StringType analyzer "autocomplete",
        "price"   typed IntegerType
      )
    )
  }

  def orders = {
    "orders_search_view" as (
      // Order
      "id"                  typed IntegerType,
      "reference_number"    typed StringType analyzer "autocomplete",
      "status"              typed StringType index "not_analyzed",
      "created_at"          typed DateType,
      "placed_at"           typed DateType,
      "currency"            typed StringType index "not_analyzed",
      // Totals
      "sub_total"           typed IntegerType,
      "shipping_total"      typed IntegerType,
      "adjustments_total"   typed IntegerType,
      "taxes_total"         typed IntegerType,
      "grand_total"         typed IntegerType,
      // Customer
      "customer"            nested (
        "name"                  typed StringType analyzer "autocomplete",
        "email"                 typed StringType analyzer "autocomplete",
        "is_blacklisted"        typed BooleanType,
        "joined_at"             typed DateType,
        "revenue"               typed IntegerType,
        "rank"                  typed IntegerType
      ),
      // Line items
      "line_item_cont"      typed IntegerType,
      "line_items"          nested (
        "status"  typed StringType index "not_analyzed",
        "sku"     typed StringType analyzer "autocomplete",
        "name"    typed StringType analyzer "autocomplete",
        "price"   typed IntegerType
      ),
      // Payments
      "payments"            nested (
        "payment_method_type" typed StringType index "not_analyzed",
        "amount"              typed IntegerType,
        "currency"            typed StringType index "not_analyzed"
      ),
      "credit_card_count"   typed IntegerType,
      "credit_card_total"   typed IntegerType,
      "gift_card_count"     typed IntegerType,
      "gift_card_total"     typed IntegerType,
      "store_credit_count"  typed IntegerType,
      "store_credit_total"  typed IntegerType,
      // Shipments
      "shipment_count"      typed IntegerType,
      "shipments"           nested (
        "status"                  typed StringType index "not_analyzed",
        "shipping_price"          typed IntegerType,
        "admin_display_name"      typed StringType analyzer "autocomplete",
        "storefront_display_name" typed StringType analyzer "autocomplete"
      ),
      // Addresses
      addresses("shipping_addresses"),
      addresses("billing_addresses"),
      // Assignments
      "assignment_count"    typed IntegerType,
      "assignees"           nested (
        "first_name"    typed StringType analyzer "autocomplete",
        "last_name"     typed StringType analyzer "autocomplete",
        "assigned_at"   typed DateType
      ),
      // RMAs
      "rma_count"           typed IntegerType,
      "rmas"                nested (
        "reference_number"  typed StringType analyzer "autocomplete",
        "status"            typed StringType index "not_analyzed",
        "rma_type"          typed StringType index "not_analyzed",
        "placed_at"         typed DateType
      )
    )
  }
}