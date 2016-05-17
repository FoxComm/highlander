create or replace function update_orders_view_from_addresses_fn() returns trigger as $$
declare order_ids int[];
begin
  case TG_TABLE_NAME
    when 'order_shipping_addresses' then
      order_ids := array_agg(NEW.order_id);
    when 'regions' then
      select array_agg(osa.order_id) into strict order_ids
      from order_shipping_addresses as osa
      inner join regions as r on (r.id = osa.region_id)
      where r.id = NEW.id;
    when 'countries' THEN
      select array_agg(osa.order_id) into strict order_ids
      from order_shipping_addresses as osa
      inner join regions as r1 on (r1.id = osa.region_id)
      inner join countries as c1 on (r1.country_id = c1.id)
      where c1.id = NEW.id;
  end case;

  update orders_search_view_test set
    shipping_addresses_count = subquery.count,
    shipping_addresses = subquery.addresses
    from (select
            o.id,
            count(osa) as count,
            case when count(osa) = 0
            then
                '[]'
            else
                json_agg((osa.address1, osa.address2, osa.city, osa.zip, r1.name, c1.name, c1.continent, c1.currency)::export_addresses)
            end as addresses
        from orders as o
        left join order_shipping_addresses as osa on (o.id = osa.order_id)
        left join regions as r1 on (osa.region_id = r1.id)
        left join countries as c1 on (r1.country_id = c1.id)
        where o.id = ANY(order_ids)) AS subquery
  WHERE orders_search_view_test.id = subquery.id;

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_order_shipping_addresses
    after update or insert on order_shipping_addresses
    for each row
    execute procedure update_orders_view_from_addresses_fn();


create trigger update_orders_view_from_regions
    after update or insert on regions
    for each row
    execute procedure update_orders_view_from_addresses_fn();

create trigger update_orders_view_from_countries
    after update or insert on countries
    for each row
    execute procedure update_orders_view_from_addresses_fn();

