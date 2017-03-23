drop trigger if exists update_orders_view_for_payments on orders;

create trigger update_orders_view_for_payments
    after insert or update on orders
    for each row
    execute procedure update_orders_view_from_payments_fn();
