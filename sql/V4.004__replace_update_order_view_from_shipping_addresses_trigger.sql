create or replace function update_orders_view_from_shipping_addresses_fn() returns trigger as $$
begin
  update orders_search_view set
    shipping_addresses_count = subquery.count,
    shipping_addresses = subquery.addresses
    from (select
            o.id,
            count(osa) as count,
            case when count(osa) = 0
            then
                '[]'
            else
                json_agg((
                  osa.address1, 
                  osa.address2, 
                  osa.city, 
                  osa.zip, 
                  r1.name, 
                  c1.name, 
                  c1.continent, 
                  c1.currency
                )::export_addresses)::jsonb
            end as addresses
        from orders as o
        left join order_shipping_addresses as osa on (o.reference_number = osa.cord_ref)
        left join regions as r1 on (osa.region_id = r1.id)
        left join countries as c1 on (r1.country_id = c1.id)
        where o.id = new.id
        group by o.id) as subquery
    where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_orders_view_from_order_shipping_addresses on order_shipping_addresses;
drop trigger if exists update_orders_view_from_regions on regions;
drop trigger if exists update_orders_view_from_countries on countries;

create trigger update_orders_view_shipping_address_when_new_order
  after insert on orders
  for each row
  execute procedure update_orders_view_from_shipping_addresses_fn();
