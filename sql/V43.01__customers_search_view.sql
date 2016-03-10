create materialized view customers_search_view as
select distinct on (c.id)
    -- Customer
    c.id as id,
    c.name as name,
    c.email as email,
    c.is_disabled as is_disabled,
    c.is_guest as is_guest,
    c.is_blacklisted as is_blacklisted,
    c.phone_number as phone_number,
    c.location as location,
    to_char(c.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as joined_at,
    -- Ranking
    rank.rank,
    coalesce(rank.revenue, 0) as revenue,
    -- Orders
    o.count as order_count,
    o.orders,
    -- Addresses
    csa.count as shipping_addresses_count,
    csa.addresses as shipping_addresses,
    cba.count as billing_addresses_count,
    cba.addresses as billing_addresses,
    -- Store credits
    sc.count as store_credit_count,
    sc.total as store_credit_total
from customers as c
inner join customer_orders_view as o on (c.id = o.customer_id)
inner join customer_shipping_addresses_view as csa on (c.id = csa.customer_id)
inner join customer_billing_addresses_view as cba on (c.id = cba.customer_id)
inner join customer_store_credit_view as sc on (c.id = sc.customer_id)
left join customers_ranking as rank on (c.id = rank.id)
order by c.id;

create unique index customers_search_view_idx on customers_search_view (id);
