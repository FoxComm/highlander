create table shipment_line_items (
  id serial primary key,
  shipment_id integer references shipments(id) on update restrict on delete restrict,
  name generic_string not null,
  reference_number generic_string not null,
  sku generic_string not null,
  price integer not null,
  image_path generic_string not null,
  -- TODO: Add relation to stock_item back
  -- stock_item_id integer not null references stock_items(id) on update restrict on delete restrict,
  state shipment_state,
  -- TODO: Add reason back
  -- reason shipment_failure_reason

  created_at generic_timestamp_now,
  updated_at generic_timestamp_now,
  deleted_at generic_timestamp_null,

  foreign key (shipment_id) references shipments on update restrict on delete restrict
);

create index shipments_id_shipments_line_items_idx on shipment_line_items (id);
