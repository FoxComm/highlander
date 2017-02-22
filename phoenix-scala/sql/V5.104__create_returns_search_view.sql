create table returns_search_view
(
    id bigint not null unique,
    reference_number reference_number not null unique,
    order_id bigint not null unique,
    state generic_string not null,
    customer jsonb not null

--    line_item_count bigint not null default 0,
--    line_items jsonb not null default '[]',
--    payments jsonb not null default '[]',
--    credit_card_count bigint not null default 0,
--    credit_card_total bigint not null default 0,
--    gift_card_count bigint not null default 0,
--    gift_card_total bigint not null default 0,
--    store_credit_count bigint not null default 0,
--    store_credit_total bigint not null default 0,
--    shipment_count bigint not null default 0,
--    shipments jsonb not null default '[]',
--    shipping_addresses_count bigint not null default 0,
--    shipping_addresses jsonb not null default '[]',
--    billing_addresses_count bigint not null default 0,
--    billing_addresses jsonb not null default '[]',
--    assignment_count bigint not null default 0,
--    assignees jsonb not null default '[]'
);

create index returns_search_view_customer_idx on returns_search_view((customer->>'id'));
create index returns_search_view_reference_number_idx on returns_search_view(reference_number);
create index returns_search_view_order_id_idx on returns_search_view(order_id);

