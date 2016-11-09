drop trigger if exists update_orders_view_for_billing_addresses on orders;
drop trigger if exists update_orders_view_shipping_address_when_new_order on orders;

create trigger update_orders_view_shipping_address_when_new_order
    after insert on order_shipping_addresses
    for each row
    execute procedure update_orders_view_from_shipping_addresses_fn();

create trigger update_orders_view_for_billing_addresses_from_payments
    after insert on order_payments
    for each row
    execute procedure update_orders_view_from_billing_addresses_fn();

create trigger update_orders_view_for_billing_addresses_from_credit_cards
    after insert on credit_cards
    for each row
    execute procedure update_orders_view_from_billing_addresses_fn();
