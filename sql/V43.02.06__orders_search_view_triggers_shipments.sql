create or replace function update_orders_view_from_shipments_fn() returns trigger as $$
declare order_refs text[];
begin
  case TG_TABLE_NAME
    when 'shipments' then
      order_refs := array_agg(NEW.order_ref);
    when 'shipping_methods' then
      select array_agg(shipments.order_ref) into strict order_refs
      from shipments
      inner join shipping_methods as sm on (shipments.order_shipping_method_id = sm.id)
      where sm.id = NEW.id;
  end case;

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
              json_agg((shipments.state, shipments.shipping_price, sm.admin_display_name, sm.storefront_display_name)
              ::export_shipments)::jsonb
            end as shipments
          from orders as o
          left join shipments on (o.reference_number = shipments.order_ref)
          left join shipping_methods as sm on (shipments.order_shipping_method_id = sm.id)
          where o.reference_number = ANY(order_refs)
          group by o.id) AS subquery
  WHERE orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_shipments
    after update or insert on shipments
    for each row
    execute procedure update_orders_view_from_shipments_fn();

create trigger update_orders_view_from_shipping_methods
    after update or insert on shipping_methods
    for each row
    execute procedure update_orders_view_from_shipments_fn();
