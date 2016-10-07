-- utility columns
alter table inventory_transactions_search_view add column stock_item_id integer references stock_items(id) on update restrict on delete restrict;
alter table inventory_transactions_search_view add column stock_location_id integer references stock_locations(id) on update restrict on delete restrict;