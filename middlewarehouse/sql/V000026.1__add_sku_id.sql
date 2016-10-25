alter table stock_items rename sku to sku_code;
alter table stock_items add column sku_id integer not null;

alter table shipment_line_items rename sku to sku_code;
alter table shipment_line_items add column sku_id integer not null;

alter table inventory_search_view rename sku to sku_code;
alter table inventory_search_view add column sku_id integer not null;

alter table inventory_transactions_search_view rename sku to sku_code;
alter table inventory_transactions_search_view add column sku_id integer not null;
