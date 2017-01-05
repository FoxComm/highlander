alter table stock_items add column sku_id integer not null references skus(id) on update restrict on delete restrict;
alter table stock_items add foreign key (sku_id) references skus(id) on update restrict on delete restrict;
alter table stock_items drop column sku;