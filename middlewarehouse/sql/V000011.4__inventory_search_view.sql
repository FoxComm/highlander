create table inventory_search_view
(
    id serial not null unique,
    sku sku_code,
    stock_item jsonb not null default '{}',
    stock_location jsonb not null default '{}',
    type stock_item_type not null,
    on_hand integer not null,
    on_hold integer not null,
    reserved integer not null,
    shipped integer not null,
    afs integer not null,
    afs_cost integer not null,
    created_at text,
    updated_at text,
    deleted_at text
);