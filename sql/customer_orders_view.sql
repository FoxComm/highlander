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

create temporary table sku_later (
    sku     generic_string,
    name    generic_string,
    price   integer
)

create materialized view customers_orders_view as
select
    c.id as id,
    c.name as name,
    c.email as email,
    c.is_disabled as is_disabled,
    c.is_guest as is_guest,
    c.is_blacklisted as is_blacklisted,
    to_char(c.created_at, 'YYYY-MM-dd') as date_joined,
    -- Revenue + ranking
    coalesce(max(rank.revenue), 0) as revenue,
    coalesce(max(rank.rank), 0) as rank
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
    -- Store credits
    count(sc.*) as store_credit_count,
    coalesce(sum(sc.available_balance), 0) as store_credit_total,
    -- Save for later
    case when count(sku_later) = 0
    then
        '[]'
    else
        json_agg((sku_later.sku, sku_later.name, sku_later.price)::tmp_sku_later)
    end as save_for_later    
from customers as c
-- Revenue + ranking
left join customers_ranking as rank on (c.id = rank.id)
-- Shipping addresses
left join addresses as a on (c.id = a.customer_id)
left join regions as r1 on (r1.id = a.region_id)
left join countries as c1 on (c1.id = r1.country_id)
-- Billing addresses
left join credit_cards as cc on (c.id = cc.customer_id)
left join regions as r2 on (r2.id = cc.region_id)
left join countries as c2 on (c2.id = r2.country_id)
-- Store credits
left join store_credits as sc on (c.id = sc.customer_id)
-- Save for later
left join save_for_later as later on (c.id = later.customer_id)
left join skus as sku_later on (later.sku_id = sku_later.id)
group by c.id;

create unique index customer on customers_orders_view (id);

select * from customers_orders_view;