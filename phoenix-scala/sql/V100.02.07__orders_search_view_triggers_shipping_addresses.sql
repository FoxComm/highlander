create or replace function update_orders_view_from_shipping_addresses_fn() returns trigger as $$
declare cord_refs text[];
begin
  case tg_table_name
    when 'order_shipping_addresses' then
      cord_refs := array_agg(new.cord_ref);
    when 'regions' then
      select array_agg(osa.cord_ref) into strict cord_refs
      from order_shipping_addresses as osa
      inner join regions as r on (r.id = osa.region_id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(osa.cord_ref) into strict cord_refs
      from order_shipping_addresses as osa
      inner join regions as r1 on (r1.id = osa.region_id)
      inner join countries as c1 on (r1.country_id = c1.id)
      where c1.id = new.id;
  end case;

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
        where o.reference_number = any(cord_refs)
        group by o.id) as subquery
    where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create trigger update_orders_view_from_order_shipping_addresses
    after update or insert on order_shipping_addresses
    for each row
    execute procedure update_orders_view_from_shipping_addresses_fn();

create trigger update_orders_view_from_regions
    after update or insert on regions
    for each row
    execute procedure update_orders_view_from_shipping_addresses_fn();

create trigger update_orders_view_from_countries
    after update or insert on countries
    for each row
    execute procedure update_orders_view_from_shipping_addresses_fn();
