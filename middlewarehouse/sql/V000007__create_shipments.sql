create table shipments (
  id serial primary key,
  method_id integer not null references shipping_methods(id) on update restrict on delete restrict,
  reference_number generic_string not null,
  state shipment_state,
  shipment_date generic_timestamp_null,
  estimated_arrival generic_timestamp_null,
  delivered_date generic_timestamp_null,
  address_id integer not null references addresses(id) on update restrict on delete restrict,
  tracking_number generic_string,

  created_at generic_timestamp_now,
  updated_at generic_timestamp_now,
  deleted_at generic_timestamp_null,

  foreign key (address_id) references addresses(id) on update restrict on delete restrict
);

create index shipments_reference_number_idx on shipments (lower(reference_number));
