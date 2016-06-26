create table stock_items (
  id serial primary key,
  stock_location_id integer not null,

  created_at timestamp without time zone default (now() at time zone 'utc'),
  updated_at timestamp without time zone default (now() at time zone 'utc'),
  deleted_at timestamp without time zone null
);
