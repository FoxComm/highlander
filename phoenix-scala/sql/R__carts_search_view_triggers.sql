create or replace function update_carts_view_from_shipments_fn() returns trigger as $$
declare cord_refs text[];
begin
  case tg_table_name
    when 'shipments' then
      cord_refs := array_agg(new.cord_ref);
    when 'shipping_methods' then
      select array_agg(shipments.cord_ref) into strict cord_refs
      from shipments
      inner join shipping_methods as sm on (shipments.order_shipping_method_id = sm.id)
      where sm.id = new.id;
  end case;

  update carts_search_view set
    shipment_count = subquery.shipment_count,
    shipments = subquery.shipments
    from (select
            c.id,
            count(shipments.id) as shipment_count,
            case when count(shipments) = 0
              then
                '[]'
            else
              json_agg((shipments.state, shipments.shipping_price, sm.name)
              ::export_shipments)::jsonb
            end as shipments
          from carts as c
          left join shipments on (c.reference_number = shipments.cord_ref)
          left join shipping_methods as sm on (shipments.order_shipping_method_id = sm.id)
          where c.reference_number = any(cord_refs)
          group by c.id) as subquery
  where carts_search_view.id = subquery.id;

  return null;
end;
$$ language plpgsql;
