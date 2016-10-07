alter table stock_items add constraint sku_location_contraint unique (sku, stock_location_id);
