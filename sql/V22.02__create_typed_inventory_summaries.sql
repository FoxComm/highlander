create table sellable_inventory_summaries (
    id serial primary key,
    on_hand integer not null default 0,
    on_hold integer not null default 0,
    reserved integer not null default 0,
    safety_stock integer not null default 0
);

create table preorder_inventory_summaries (
    id serial primary key,
    on_hand integer not null default 0,
    on_hold integer not null default 0,
    reserved integer not null default 0
);

create table backorder_inventory_summaries (
    id serial primary key,
    on_hand integer not null default 0,
    on_hold integer not null default 0,
    reserved integer not null default 0
);

create table nonsellable_inventory_summaries (
    id serial primary key,
    on_hand integer not null default 0,
    on_hold integer not null default 0,
    reserved integer not null default 0
);
