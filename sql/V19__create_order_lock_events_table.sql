create table cart_lock_events (
  id serial primary key,
  cart_ref text references cords(reference_number) on update restrict on delete restrict,
  locked_at generic_timestamp,
  locked_by int references store_admins(id) on update restrict on delete restrict
);

