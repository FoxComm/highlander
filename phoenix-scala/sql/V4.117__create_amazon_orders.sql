create table amazon_orders(
    id bigserial primary key,
    amazon_order_id generic_string,
    order_total integer not null default 0,
    payment_method_detail generic_string,
    order_type generic_string,
    currency currency,
    order_status generic_string,
    purchase_date generic_timestamp,
    scope ltree,
    customer_name generic_string,
    customer_email generic_string,
    updated_at generic_timestamp,
    created_at generic_timestamp
);
