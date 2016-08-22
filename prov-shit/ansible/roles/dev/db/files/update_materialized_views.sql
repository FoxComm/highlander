-- customers
refresh materialized view concurrently customers_ranking;

-- orders
refresh materialized view concurrently order_stats_view;

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
refresh materialized view concurrently notes_admins_view;
refresh materialized view concurrently notes_customers_view;
refresh materialized view concurrently notes_gift_cards_view;
refresh materialized view concurrently notes_skus_view;
refresh materialized view concurrently notes_products_view;
refresh materialized view concurrently notes_promotions_view;
refresh materialized view concurrently notes_coupons_view;
refresh materialized view concurrently notes_search_view;

-- pim
refresh materialized view concurrently customer_purchased_items_view;
refresh materialized view concurrently customer_save_for_later_view;
alter sequence customer_items_view_seq restart 1;
refresh materialized view concurrently customer_items_view;

-- promotions
refresh materialized view concurrently promotion_discount_links_view;
refresh materialized view concurrently promotions_search_view;
