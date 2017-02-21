drop index if exists return_line_item_shipments_shipment_idx;
drop index if exists return_line_item_shipments_return_idx;

alter table return_line_item_shipments
  drop constraint if exists return_line_item_shipments_shipment_id_fkey,
  drop column shipment_id;

alter table return_line_item_shipments
  add column line_item_id int,
  add column amount int;

alter table return_line_item_shipments rename to return_line_item_shipping_costs;

create index return_line_item_shipping_costs_return_idx on return_line_item_shipping_costs (return_id);
