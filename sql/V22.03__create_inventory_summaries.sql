create table inventory_summaries (
    id serial primary key,
    sku_id integer not null references skus(id) on update restrict on delete restrict,
    warehouse_id integer not null references warehouses(id) on update restrict on delete restrict,
    sellable_id integer not null references sellable_inventory_summaries(id) on update restrict on delete restrict,
    backorder_id integer not null references backorder_inventory_summaries(id) on update restrict on delete restrict,
    preorder_id integer not null references preorder_inventory_summaries(id) on update restrict on delete restrict,
    nonsellable_id integer not null references nonsellable_inventory_summaries(id) on update restrict on delete restrict
);

create unique index inventory_summaries_sku_warehouse_idx on inventory_summaries(sku_id, warehouse_id);
