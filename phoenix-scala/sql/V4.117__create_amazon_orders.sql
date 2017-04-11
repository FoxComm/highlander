create table amazon_orders(
    id bigserial primary key,
    amazon_order_id generic_string,
    order_total integer not null default 0,
    payment_method_detail generic_string,
    order_type generic_string,
    currency currency,
    order_status generic_string,
    purchased_date timestamp,
    updated_at timestamp,
    created_at timestamp
)