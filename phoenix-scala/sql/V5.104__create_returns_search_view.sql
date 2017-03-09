create table returns_search_view
(
    id bigint not null unique,
    reference_number reference_number not null unique,
    order_id bigint not null REFERENCES orders_search_view(id),
    order_ref reference_number not null,
    created_at text,
    state return_state not null,
    total_refund integer default 0,
    message_to_account text,
    return_type return_type,
    customer jsonb
    -- TODO add more fields as needed
);

create index returns_search_view_customer_idx on returns_search_view((customer->>'id'));
create index returns_search_view_reference_number_idx on returns_search_view(reference_number);
create index returns_search_view_order_id_idx on returns_search_view(order_id);

