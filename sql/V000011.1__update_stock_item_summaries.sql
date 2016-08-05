create domain stock_item_type text not null check (value in ('Sellable', 'Non-sellable', 'Backorder', 'Preorder'));

alter table stock_item_summaries add column type stock_item_type not null;
alter table stock_item_summaries add column shipped integer not null default 0;
alter table stock_item_summaries add column safety_stock integer not null default 0;
alter table stock_item_summaries add column afs integer not null default 0;
alter table stock_item_summaries add column afs_cost integer not null default 0;

alter table stock_item_summaries alter column on_hand set default 0;
alter table stock_item_summaries alter column on_hold set default 0;
alter table stock_item_summaries alter column reserved set default 0;