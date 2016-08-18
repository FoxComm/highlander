-- create constraint on (stock_item_id, type) pair
alter table stock_item_summaries add constraint stock_item_id_type_constraint unique (stock_item_id, type);
