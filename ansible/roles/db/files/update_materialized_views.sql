-- customers
refresh materialized view concurrently customers_ranking;
refresh materialized view concurrently customer_orders_view;
refresh materialized view concurrently customer_purchased_items_view;
refresh materialized view concurrently customer_shipping_addresses_view;
refresh materialized view concurrently customer_billing_addresses_view;
refresh materialized view concurrently customer_store_credit_view;
refresh materialized view concurrently customer_save_for_later_view;
refresh materialized view concurrently customers_search_view;

-- orders
refresh materialized view concurrently order_line_items_view;
refresh materialized view concurrently order_payments_view;
refresh materialized view concurrently order_credit_card_payments_view;
refresh materialized view concurrently order_gift_card_payments_view;
refresh materialized view concurrently order_store_credit_payments_view;
refresh materialized view concurrently order_shipments_view;
refresh materialized view concurrently order_shipping_addresses_view;
refresh materialized view concurrently order_billing_addresses_view;
refresh materialized view concurrently order_assignments_view;
refresh materialized view concurrently order_rmas_view;
refresh materialized view concurrently orders_search_view;

-- store_admins
refresh materialized view concurrently store_admin_assignments_view;
refresh materialized view concurrently store_admins_search_view;

-- store_credits
refresh materialized view concurrently store_credit_admins_view;
refresh materialized view concurrently store_credit_from_gift_cards_view;
refresh materialized view concurrently store_credit_subtypes_view;
refresh materialized view concurrently store_credit_cancellation_reasons_view;
refresh materialized view concurrently store_credits_search_view;

-- gift_cards
refresh materialized view concurrently gift_card_admins_view;
refresh materialized view concurrently gift_card_from_store_credits_view;
refresh materialized view concurrently gift_card_subtypes_view;
refresh materialized view concurrently gift_card_cancellation_reasons_view;
refresh materialized view concurrently gift_cards_search_view;

-- gift_card_transactions
refresh materialized view concurrently gift_card_transactions_admins_view;
refresh materialized view concurrently gift_card_transactions_payments_view;
refresh materialized view concurrently gift_card_transactions_view;

-- store_credit_transactions
refresh materialized view concurrently store_credit_transactions_admins_view;
refresh materialized view concurrently store_credit_transactions_payments_view;
refresh materialized view concurrently store_credit_transactions_view;

-- failed authorizations
refresh materialized view concurrently failed_authorizations_search_view;

-- notes
refresh materialized view concurrently notes_orders_view;
refresh materialized view concurrently notes_customers_view;
refresh materialized view concurrently notes_gift_cards_view;
refresh materialized view concurrently notes_admins_view;
refresh materialized view concurrently notes_search_view;

-- inventory
refresh materialized view concurrently inventory_search_view;
refresh materialized view concurrently sku_search_view;

-- locations
refresh materialized view concurrently regions_search_view;
refresh materialized view concurrently countries_search_view;
