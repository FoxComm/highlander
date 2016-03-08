create table inventory_adjustments (
  id serial primary key,
  sku_id integer not null references skus(id) on update restrict on delete restrict,
  warehouse_id integer not null references warehouses(id) on update restrict on delete restrict,
  sellable_id integer not null references sellable_inventory_adjustments(id) on update restrict on delete restrict,
  backorder_id integer not null references backorder_inventory_adjustments(id) on update restrict on delete restrict,
  preorder_id integer not null references preorder_inventory_adjustments(id) on update restrict on delete restrict,
  nonsellable_id integer not null references nonsellable_inventory_adjustments(id) on update restrict on delete restrict,
  created_at timestamp without time zone default (now() at time zone 'utc')
);

create unique index inventory_adjustments_sku_warehouse_idx on inventory_adjustments(sku_id, warehouse_id);
