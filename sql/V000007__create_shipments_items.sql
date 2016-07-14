create table shipments_items (
  id serial primary key,
  shipment_id integer not null references shipments(id) on update restrict on delete restrict,
  stock_item_id integer not null references stock_items(id) on update restrict on delete restrict,
  quantity integer not null,
  state generic_string
);
