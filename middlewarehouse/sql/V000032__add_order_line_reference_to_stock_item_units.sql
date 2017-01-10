alter table stock_item_units add column order_line_item_ref_num generic_string null;
alter table stock_item_units rename column ref_num to order_ref_num;

alter table stock_item_units add constraint chk_stock_items_units_ref_nums check (
    (order_ref_num is null and order_line_item_ref_num is null) or
    (order_ref_num is not null and order_line_item_ref_num is not null));