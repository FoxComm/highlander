alter table return_line_items drop column is_return_item;

drop table return_line_item_gift_cards;

drop trigger if exists set_return_line_item_sku_id on return_line_item_skus;
drop trigger if exists set_return_line_item_shipment_id on return_line_item_shipping_costs;
drop function if exists set_return_line_item_origin_id();
alter table return_line_items drop constraint if exists return_line_items_origin_id_fkey;
alter table return_line_item_shipping_costs drop constraint if exists return_line_item_shipments_id_fkey;
alter table return_line_item_skus drop constraint if exists return_line_item_skus_id_fkey;
drop table return_line_item_origins;
