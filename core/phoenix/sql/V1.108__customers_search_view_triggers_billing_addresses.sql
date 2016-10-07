create or replace function update_customers_view_from_billing_addresses_fn() returns trigger as $$
declare customer_ids integer[];
begin
  case tg_table_name
    when 'credit_cards' then
      customer_ids := array_agg(new.customer_id);
    when 'regions' then
      select array_agg(cc.customer_id) into strict customer_ids
      from credit_cards as cc
      inner join regions as r on (cc.region_id = r.id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(o.customer_id) into strict customer_ids
      from credit_cards as cc
      inner join regions as r on (cc.region_id = r.id)
      inner join countries as c on (c.id = r.country_id)
      where c.id = new.id;
  end case;

  update customers_search_view set
    billing_addresses_count = subquery.count,
    billing_addresses = subquery.addresses
    from (select
            c.id,
            count(cc) as count,
            case when count(cc) = 0
            then
                '[]'
            else
                json_agg((
                  cc.address1, 
                  cc.address2, 
                  cc.city, 
                  cc.zip, 
                  r2.name, 
                  c2.name, 
                  c2.continent, 
                  c2.currency
                )::export_addresses)::jsonb
            end as addresses
        from customers as c 
        left join credit_cards as cc on (cc.customer_id = c.id)
        left join regions as r2 on (cc.region_id = r2.id)
        left join countries as c2 on (r2.country_id = c2.id)
        where c.id = any(customer_ids)
        group by c.id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create trigger update_customers_view_from_billing_addresses_credit_cards
    after update or insert on credit_cards
    for each row
    execute procedure update_customers_view_from_billing_addresses_fn();

create trigger update_customers_view_from_billing_addresses_countries
    after update or insert on countries
    for each row
    execute procedure update_customers_view_from_billing_addresses_fn();

create trigger update_customers_view_from_billing_addresses_regions
    after update or insert on regions
    for each row
    execute procedure update_customers_view_from_billing_addresses_fn();
