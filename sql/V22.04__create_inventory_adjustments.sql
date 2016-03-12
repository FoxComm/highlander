create table inventory_adjustments (
    id serial primary key,
    summary_id int not null,
    metadata jsonb null,
    change int not null default 0,
    new_quantity int not null default 0,
    new_afs int not null default 0,
    state generic_string not null,
    sku_type generic_string not null,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

-- TODO: validation for 'state' and 'sku_type'
