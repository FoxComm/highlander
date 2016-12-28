create or replace function update_orders_view_from_shipment_methods_fn() returns trigger as $$
begin
    update orders_search_view set
        shipping_method = subquery.shipping_method
        from (
            select json_build_object(
                'id', osm.id,
                'shipping_method_id', sm.id,
                'name', sm.name,
                'carrier', sm.carrier,
                'eta', sm.eta,
                'price', osm.price)::jsonb as shipping_method
            from orders as o
            left join order_shipping_methods as osm on (o.reference_number = osm.cord_ref)
            left join shipping_methods as sm on (osm.shipping_method_id = sm.id)
            where o.id = new.id) as subquery
    where orders_search_view.id = new.id;

    return null;
end;
$$ language plpgsql;

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
                       sm.name)::export_shipments)::jsonb
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
