create table amazon_orders(
    id bigserial primary key,
    amazon_order_id generic_string,
    order_total integer not null default 0,
    payment_method_detail generic_string,
    order_type generic_string,
    currency currency,
    order_status generic_string,
    purchase_date generic_timestamp,
    updated_at generic_timestamp,
    created_at generic_timestamp
);

create table amazon_orders_search_view (
  id bigint primary key,
    amazon_order_id generic_string,
    order_total integer not null default 0,
    payment_method_detail generic_string,
    order_type generic_string,
    currency currency,
    order_status generic_string,
    purchase_date json_timestamp,
    updated_at json_timestamp,
    created_at json_timestamp
);
