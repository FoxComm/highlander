create or replace function update_customers_view_from_shipping_addresses_fn() returns trigger as $$
declare customer_ids integer[];
begin
  case TG_TABLE_NAME
    when 'addresses' then
      customer_ids := array_agg(new.customer_id);
    when 'regions' then
      select array_agg(o.customer_id) into strict customer_ids
      from addresses as a
      inner join regions as r on (r.id = a.region_id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(o.customer_id) into strict customer_ids
      from addresses as a
      inner join regions as r1 on (r1.id = a.region_id)
      inner join countries as c1 on (r1.country_id = c1.id)
      where c1.id = new.id;
  end case;

  update customers_search_view set
    shipping_addresses_count = subquery.count,
    shipping_addresses = subquery.addresses
    from (select
            c.id,
            count(a) as count,
            case when count(a) = 0
            then
                '[]'
            else
                json_agg((
                  a.address1, 
                  a.address2, 
                  a.city, 
                  a.zip, 
                  r1.name, 
                  c1.name, 
                  c1.continent, 
                  c1.currency
                )::export_addresses)::jsonb
            end as addresses
        from customers as c
        left join addresses as a on (a.customer_id = c.id)
        left join regions as r1 on (a.region_id = r1.id)
        left join countries as c1 on (r1.country_id = c1.id)
        where c.id = any(customer_ids)
        group by c.id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create trigger update_customers_view_from_order_shipping_addresses
    after update or insert on addresses
    for each row
    execute procedure update_customers_view_from_shipping_addresses_fn();

create trigger update_customers_view_from_regions
    after update or insert on regions
    for each row
    execute procedure update_customers_view_from_shipping_addresses_fn();

create trigger update_customers_view_from_countries
    after update or insert on countries
    for each row
    execute procedure update_customers_view_from_shipping_addresses_fn();
