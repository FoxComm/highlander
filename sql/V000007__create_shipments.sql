create table shipments (
  id serial primary key,
  method_id integer not null references shipment_methods(id) on update restrict on delete restrict,
  state shipment_state,
  shipment_date generic_timestamp_null,
  estimated_arrival generic_timestamp_null,
  delivered_date generic_timestamp_null,
  tracking_number generic_string,
  address jsonb not null
);
