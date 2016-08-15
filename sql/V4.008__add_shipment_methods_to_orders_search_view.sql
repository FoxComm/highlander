alter table orders_search_view add column shipping_method jsonb not null default '{}';

create or replace function update_orders_view_from_shipment_methods_fn() returns trigger as $$
begin
    update orders_search_view set 
        shipping_method = subquery.shipping_method 
        from (
            select json_build_object(
                'id', osm.id,
                'shipping_method_id', sm.id,
                'admin_display_name', sm.admin_display_name,
                'storefront_display_name', sm.storefront_display_name,
                'price', osm.price)::jsonb as shipping_method
            from orders as o
            left join order_shipping_methods as osm on (o.reference_number = osm.cord_ref)
            left join shipping_methods as sm on (osm.shipping_method_id = sm.id)
            where o.id = new.id) as subquery
    where orders_search_view.id = new.id;

    return null;
end;
$$ language plpgsql;

create trigger update_orders_view_from_shipping_methods
    after insert on orders_search_view
    for each row
    execute procedure update_orders_view_from_shipment_methods_fn();
