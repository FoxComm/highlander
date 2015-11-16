create table inventory_summaries (
    id serial primary key,
    warehouse_id integer not null,
    sku_id integer not null,
    on_hand integer not null default 0,
    on_hold integer not null default 0,
    reserved integer not null default 0,
    non_sellable integer not null default 0,
    safety_stock integer not null default 0,
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (sku_id) references skus(id) on update restrict on delete restrict,
    foreign key (warehouse_id) references warehouses(id) on update restrict on delete restrict
);

