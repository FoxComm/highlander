create table stock_item_types (
  id serial primary key,
  type generic_string not null,
  description generic_string null
);