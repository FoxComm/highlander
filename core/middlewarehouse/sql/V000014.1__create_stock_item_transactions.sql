-- fix default for stock_item_units.type value
alter table stock_item_units alter column type set default 'Sellable';

-- create stock_item_transactions table to track units quantity change history
create table stock_item_transactions (
  id serial primary key,
  stock_item_id integer not null references stock_items(id) on update restrict on delete restrict,
  type stock_item_type not null,
  status stock_item_unit_state not null,
  quantity_new integer not null,
  quantity_change integer not null,
  afs_new integer not null,

  created_at generic_timestamp_now
);
