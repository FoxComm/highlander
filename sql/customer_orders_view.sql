drop materialized view if exists customers_orders_view;

create temporary table if not exists tmp_addresses (
    address1            generic_string,
    address2            generic_string,
    city                generic_string,
    zip                 zip_code,
    region_name         generic_string,
    country_name        generic_string,
    country_continent   generic_string,
    country_currency    currency
);

create temporary table if not exists tmp_skus (
    sku     generic_string,
    name    generic_string,
    price   integer
);

create temporary table if not exists tmp_orders (
    reference_number generic_string,
    status           generic_string,
    date_placed      generic_string   
);

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
    coalesce(max(rank.rank), 0) as rank,
    -- Store credits
    count(distinct sc.id) as store_credit_count,
    coalesce(sum(distinct sc.available_balance), 0) as store_credit_total,
    -- Orders
    count(distinct o1.id) as order_count,
    case when count(distinct o1) = 0
    then
        '[]'
    else
        json_agg(distinct (o1.reference_number, o1.status, to_char(o1.created_at, 'YYYY-MM-dd'))::tmp_orders)
    end as orders,
    -- Purchased items
    count(distinct s.id) as purchased_items_count,
    case when count(distinct s) = 0
    then
        '[]'
    else
        json_agg(distinct (s.sku, s.name, s.price)::tmp_skus)
    end as purchased_items,    
    -- Shipping addresses
    case when count(distinct a) = 0
    then
        '[]'
    else
        json_agg(distinct (a.address1, a.address2, a.city, a.zip, r1.name, c1.name, c1.continent, c1.currency)::tmp_addresses)
    end as shipping_addresses,
    -- Billing addresses
    case when count(distinct a) = 0
    then
        '[]'
    else
        json_agg(distinct (cc.address1, cc.address2, cc.city, cc.zip, r2.name, c2.name, c2.continent, c2.currency)::tmp_addresses)
    end as billing_addresses,
    -- Saved for later
    count(distinct sku_later.id) as saved_for_later_count,
    case when count(sku_later) = 0
    then
        '[]'
    else
        json_agg(distinct (sku_later.sku, sku_later.name, sku_later.price)::tmp_skus)
    end as save_for_later  
from customers as c
-- Orders + RMAs
left join orders as o1 on (c.id = o1.customer_id)
-- Purchased items
left join orders as o2 on (c.id = o2.customer_id and o2.status = 'shipped')
left join order_line_items as oli on (o2.id = oli.order_id and oli.status = 'shipped')
left join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
left join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
left join skus as s on (oli_skus.sku_id = s.id)
-- Revenue + ranking
left join customers_ranking as rank on (c.id = rank.id)
-- Store credits
left join store_credits as sc on (c.id = sc.customer_id)
-- Shipping addresses
left join addresses as a on (c.id = a.customer_id)
left join regions as r1 on (r1.id = a.region_id)
left join countries as c1 on (c1.id = r1.country_id)
-- Billing addresses
left join credit_cards as cc on (c.id = cc.customer_id)
left join regions as r2 on (r2.id = cc.region_id)
left join countries as c2 on (c2.id = r2.country_id)
-- Saved for later
left join save_for_later as later on (c.id = later.customer_id)
left join skus as sku_later on (later.sku_id = sku_later.id)
group by c.id;

create unique index customer on customers_orders_view (id);

select * from customers_orders_view;