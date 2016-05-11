create table orders_search_view
(
    id bigint not null unique,
    reference_number reference_number not null unique,
    state generic_string not null,
    created_at text,
    placed_at text,
    currency currency,
    sub_total integer not null default 0,
    shipping_total integer not null default 0,
    adjustments_total integer not null default 0,
    taxes_total integer not null default 0,
    grand_total integer not null default 0,
    customer jsonb not null,
    line_item_count bigint,
    line_items jsonb,
    payments jsonb,
    credit_card_count bigint,
    credit_card_total bigint,
    gift_card_count bigint,
    gift_card_total bigint,
    store_credit_count bigint,
    store_credit_total bigint,
    shipment_count bigint,
    shipments jsonb,
    shipping_addresses_count bigint,
    shipping_addresses jsonb,
    billing_addresses_count bigint,
    billing_addresses jsonb,
    assignment_count bigint,
    assignees jsonb,
    rma_count bigint,
    rmas jsonb
);

