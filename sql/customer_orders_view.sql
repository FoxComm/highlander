drop materialized view if exists customers_orders_view;

create temporary table tmp_addresses (
    address1            generic_string,
    address2            generic_string,
    city                generic_string,
    zip                 zip_code,
    region_name         generic_string,
    country_name        generic_string,
    country_continent   generic_string,
    country_currency    currency
);

create materialized view customers_orders_view as
select
    c.id,
    c.name,
    c.email,
    c.is_blacklisted,
    to_char(c.created_at, 'YYYY-MM-dd') as registration_date,
    -- Shipping addresses
    case when count(a) = 0
    then
        '[]'
    else
        json_agg((a.address1, a.address2, a.city, a.zip, r1.name, c1.name, c1.continent, c1.currency)::tmp_addresses)
    end as shipping_addresses,
    -- Billing addresses
    case when count(a) = 0
    then
        '[]'
    else
        json_agg((cc.address1, cc.address2, cc.city, cc.zip, r2.name, c2.name, c2.continent, c2.currency)::tmp_addresses)
    end as billing_addresses,
    -- Store credit aggregations
    count(sc.*) as store_credit_count,
    coalesce(sum(sc.available_balance), 0) as store_credit_total
from customers as c
-- Shipping addresses
left join addresses as a on (c.id = a.customer_id)
left join regions as r1 on (r1.id = a.region_id)
left join countries as c1 on (c1.id = r1.country_id)
-- Billing addresses
left join credit_cards as cc on (c.id = cc.customer_id)
left join regions as r2 on (r2.id = cc.region_id)
left join countries as c2 on (c2.id = r2.country_id)
-- Store credit stats
left join store_credits as sc on (c.id = sc.customer_id)
group by c.id;

create unique index customer on customers_orders_view (id);

select * from customers_orders_view;