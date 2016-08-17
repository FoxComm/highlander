-- fix default for stock_item_units.type value
alter table stock_item_units alter column type set default 'Sellable';
-- remove obsolete foreign keys
alter table stock_item_summaries drop constraint stock_item_summaries_stock_item_id_fkey1;
alter table stock_item_units drop constraint stock_item_units_stock_item_id_fkey1;
alter table shipments drop constraint shipments_address_id_fkey1;
alter table shipment_line_items drop constraint shipment_line_items_shipment_id_fkey1;
alter table addresses drop constraint addresses_region_id_fkey1;

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
