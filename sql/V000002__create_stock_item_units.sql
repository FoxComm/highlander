create table stock_item_units (
  id serial primary key,
  unit_cost integer not null,
  stock_item_id integer not null references stock_items(id) on update restrict on delete restrict,
  status generic_string,

  created_at generic_timestamp,
  updated_at generic_timestamp,
  deleted_at timestamp without time zone null,
  foreign key (stock_item_id) references stock_items(id) on update restrict on delete restrict
);

create index stock_item_unit_idx on stock_item_units (stock_item_id);
