create or replace function update_orders_view_from_shipments_fn() returns trigger as $$
begin
  update orders_search_view set
    shipment_count = subquery.shipment_count,
    shipments = subquery.shipments
    from (select
            o.id,
            count(shipments.id) as shipment_count,
            case when count(shipments) = 0
              then
                '[]'
            else
              json_agg((
                  shipments.state,
                  shipments.shipping_price,
                  sm.admin_display_name,
                  sm.storefront_display_name)::export_shipments)::jsonb
            end as shipments
          from orders as o
          left join shipments on (o.reference_number = shipments.cord_ref)
          left join shipping_methods as sm on (shipments.order_shipping_method_id = sm.id)
          where o.id = new.id
          group by o.id) as subquery
  where orders_search_view.id = subquery.id;

  return null;
end;
$$ language plpgsql;

drop trigger if exists update_orders_view_from_shipments on shipments;
drop trigger if exists update_orders_view_from_shipping_methods on shipping_methods;

create trigger update_orders_view_for_shipping_methods
    after insert on orders
    for each row
    execute procedure update_orders_view_from_shipments_fn();

