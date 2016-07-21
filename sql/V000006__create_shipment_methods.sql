create table shipment_methods (
  id serial primary key,
  carrier_id integer not null references carriers on update restrict on delete restrict,
  name generic_string
);
