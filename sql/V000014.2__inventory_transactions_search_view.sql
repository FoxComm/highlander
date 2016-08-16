create table inventory_transactions_search_view
(
    id serial primary key,
    sku sku_code not null,
    stock_location_name generic_string not null,
    type stock_item_type not null,
    status stock_item_unit_state not null,
    amount_previous integer not null,
    amount_new integer not null,
    amount_change integer not null,
    afs_new integer not null,
    created_at text
);