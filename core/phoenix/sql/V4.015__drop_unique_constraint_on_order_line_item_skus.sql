alter table order_line_item_skus drop constraint order_line_item_skus_sku_id_key;
alter table order_line_item_skus drop constraint order_line_item_skus_sku_shadow_id_key;

alter table order_line_item_skus alter column sku_id set not null;
alter table order_line_item_skus add constraint order_line_item_skus_sku_id_key
  foreign key (sku_id) references skus(id) on update restrict on delete restrict;

alter table order_line_item_skus alter column sku_id set not null;
alter table order_line_item_skus add constraint order_line_item_skus_sku_shadow_id_key
  foreign key (sku_shadow_id) references object_shadows(id) on update restrict on delete restrict;
