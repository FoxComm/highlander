alter table stock_item_units add column line_item_reference generic_string null;
alter table stock_item_units rename column ref_num to order_ref_num;