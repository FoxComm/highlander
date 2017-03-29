alter table return_line_items
  drop column inventory_disposition,
  drop column is_return_item,
  drop column origin_id,
  drop column quantity,
  drop column reference_number;

alter table return_line_item_shipping_costs
  drop column line_item_id,
  add constraint return_line_item_shipping_costs_id_key foreign key (id) references return_line_items (id) on delete cascade on update restrict;

alter table return_line_item_skus
  add column quantity integer,
  add constraint return_line_item_skus_id_key foreign key (id) references return_line_items (id) on delete cascade on update restrict;

drop table return_line_item_gift_cards;

drop trigger if exists set_rli_refnum_trg on return_line_items;
drop function if exists set_rli_refnum();

drop trigger if exists set_return_line_item_sku_id on return_line_item_skus;
drop trigger if exists set_return_line_item_shipment_id on return_line_item_shipping_costs;
drop function if exists set_return_line_item_origin_id();
alter table return_line_items drop constraint if exists return_line_items_origin_id_fkey;
alter table return_line_item_shipping_costs drop constraint if exists return_line_item_shipments_id_fkey;
alter table return_line_item_skus drop constraint if exists return_line_item_skus_id_fkey;
drop table return_line_item_origins;
