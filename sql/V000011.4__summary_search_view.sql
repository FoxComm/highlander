create table summary_search_view
(
    id serial not null unique,
    stock_location jsonb not null,
    sku sku_code not null,
    type generic_string not null,
    on_hand integer not null,
    on_hold integer not null,
    reserved integer not null,
    shipped integer not null,
    afs integer not null,
    afs_count integer not null,
    created_at generic_timestamp_now,
    updated_at generic_timestamp_now
);