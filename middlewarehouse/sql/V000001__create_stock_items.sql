create table stock_items (
  id serial primary key,
  sku sku_code, 
  stock_location_id integer not null,

  created_at generic_timestamp,
  updated_at generic_timestamp,
  deleted_at timestamp without time zone null
);

create unique index skus_code_id on stock_items (lower(sku));
