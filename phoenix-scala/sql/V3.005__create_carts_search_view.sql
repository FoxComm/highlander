create table carts_search_view
(
    id bigint not null unique,
    reference_number reference_number not null unique,
    created_at text,
    updated_at text,
    currency currency,
    sub_total integer not null default 0,
    shipping_total integer not null default 0,
    adjustments_total integer not null default 0,
    taxes_total integer not null default 0,
    grand_total integer not null default 0,
    customer jsonb not null,
    line_item_count bigint not null default 0,
    line_items jsonb not null default '[]',
    payments jsonb not null default '[]',
    credit_card_count bigint not null default 0,
    credit_card_total bigint not null default 0,
    gift_card_count bigint not null default 0,
    gift_card_total bigint not null default 0,
    store_credit_count bigint not null default 0,
    store_credit_total bigint not null default 0,
    shipment_count bigint not null default 0,
    shipments jsonb not null default '[]',
    shipping_addresses_count bigint not null default 0,
    shipping_addresses jsonb not null default '[]',
    billing_addresses_count bigint not null default 0,
    billing_addresses jsonb not null default '[]',
    deleted_at text null
);

create index carts_search_view_customer_idx on carts_search_view((customer->>'id'));
