alter table stock_item_units add column type stock_item_type not null default 1;
alter table stock_item_units alter column status set default 'onHand';