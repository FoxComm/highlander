drop trigger if exists update_orders_view_for_line_items on orders;
drop trigger if exists update_orders_view_for_payments on orders;
drop trigger if exists update_orders_view_for_shipping_methods on orders;
drop trigger if exists update_orders_view_shipping_address_when_new_order on orders;
drop trigger if exists update_orders_view_for_billing_addresses on orders;

create trigger update_orders_search_view_for_line_items
    after insert on orders_search_view
    for each row
    execute procedure update_orders_view_from_line_items_fn();

create trigger update_orders_search_view_for_payments
    after insert on orders_search_view
    for each row
    execute procedure update_orders_view_from_payments_fn();

create trigger update_orders_search_view_for_shipping_methods
    after insert on orders_search_view
    for each row
    execute procedure update_orders_view_from_shipments_fn();
 
create trigger update_orders_search_view_shipping_address_when_new_order
  after insert on orders_search_view
  for each row
  execute procedure update_orders_view_from_shipping_addresses_fn();

create trigger update_orders_search_view_for_billing_addresses
    after insert on orders_search_view
    for each row
    execute procedure update_orders_view_from_billing_addresses_fn();

