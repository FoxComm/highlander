create table inventory_adjustments (
    id serial primary key,
    summary_id int not null,
    metadata jsonb null,
    change int not null default 0,
    new_quantity int not null default 0,
    new_afs int not null default 0,
    state generic_string not null,
    sku_type generic_string not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    constraint valid_state check (state in ('onHand', 'onHold', 'reserved', 'safetyStock')),
    constraint valid_sku_type check (sku_type in ('sellable', 'preorder', 'backorder', 'nonSellable'))
);
