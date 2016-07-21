create table shipments_transactions (
  id serial primary key,
  shipment_id integer not null references shipments(id) on update restrict on delete restrict,
  source jsonb not null,
  created_at generic_timestamp_now,
  amount integer not null
);
