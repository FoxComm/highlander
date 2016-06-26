create table stock_items (
  id serial primary key,
  sku generic_string not null,
  stock_location_id integer not null,

  created_at generic_timestamp,
  updated_at generic_timestamp,
  deleted_at timestamp without time zone null
);
