create table order_lock_events (
  id serial primary key,
  order_id bigint references orders(id) on update restrict on delete restrict,
  locked_on timestamp without time zone default (now() at time zone 'utc'),
  locked_by int references store_admins(id) on update restrict on delete restrict
);

