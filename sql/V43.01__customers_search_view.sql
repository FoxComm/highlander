create materialized view customers_search_view as
select distinct on (c.id)
    -- Customer
    c.id as id,
    c.name as name,
    c.email as email,
    c.is_disabled as is_disabled,
    c.is_guest as is_guest,
    c.is_blacklisted as is_blacklisted,
    to_char(c.created_at, 'YYYY-MM-dd') as joined_at,
    -- Ranking
    rank.rank,
    coalesce(rank.revenue, 0) as revenue,
    -- Orders
    o.count as order_count,
    o.orders,
    -- Purchased items
    i.count as purchased_item_count,
    i.items as purchased_items,
    -- Addresses
    csa.count as shipping_addresses_count,
    csa.addresses as shipping_addresses,
    cba.count as billing_addresses_count,
    cba.addresses as billing_addresses,
    -- Store credits
    sc.count as store_credit_count,
    sc.total as store_credit_total,
    -- Save for later
    sfl.count as save_for_later_count,
    sfl.items as save_for_later
from customers as c
inner join customer_orders_view as o on (c.id = o.customer_id)
inner join customer_purchased_items_view as i on (c.id = i.customer_id)
inner join customer_shipping_addresses_view as csa on (c.id = csa.customer_id)
inner join customer_billing_addresses_view as cba on (c.id = cba.customer_id)
inner join customer_store_credit_view as sc on (c.id = sc.customer_id)
inner join customer_save_for_later_view as sfl on (c.id = sfl.customer_id)
left join customers_ranking as rank on (c.id = rank.id)
order by c.id;

create unique index customers_search_view_idx on customers_search_view (id);