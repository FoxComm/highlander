create table sellable_inventory_adjustments (
    id serial primary key,
    summary_id int not null,
    metadata jsonb null,
    on_hand_change int not null default 0,
    on_hold_change int not null default 0,
    reserved_change int not null default 0,
    safety_stock_change int not null default 0,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create table preorder_inventory_adjustments (
    id serial primary key,
    summary_id int not null,
    metadata jsonb null,
    on_hand_change int not null default 0,
    on_hold_change int not null default 0,
    reserved_change int not null default 0,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create table backorder_inventory_adjustments (
    id serial primary key,
    summary_id int not null,
    metadata jsonb null,
    on_hand_change int not null default 0,
    on_hold_change int not null default 0,
    reserved_change int not null default 0,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create table nonsellable_inventory_adjustments (
    id serial primary key,
    summary_id int not null,
    metadata jsonb null,
    on_hand_change int not null default 0,
    on_hold_change int not null default 0,
    reserved_change int not null default 0,
    created_at timestamp without time zone default (now() at time zone 'utc')
);
