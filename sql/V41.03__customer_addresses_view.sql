create materialized view customer_addresses_view as
select
    c.id as customer_id,
    -- Shipping addresses
    case when count(a) = 0
    then
        '[]'
    else
        json_agg((a.address1, a.address2, a.city, a.zip, r1.name, c1.name, c1.continent, c1.currency)::export_addresses)
    end as shipping_addresses,
    -- Billing addresses
    case when count(cc) = 0
    then
        '[]'
    else
        json_agg((cc.address1, cc.address2, cc.city, cc.zip, r2.name, c2.name, c2.continent, c2.currency)::export_addresses)
    end as billing_addresses
from customers as c
-- Shipping addresses
left join addresses as a on (c.id = a.customer_id)
left join regions as r1 on (r1.id = a.region_id)
left join countries as c1 on (c1.id = r1.country_id)
-- Billing addresses
left join credit_cards as cc on (c.id = cc.customer_id)
left join regions as r2 on (r2.id = cc.region_id)
left join countries as c2 on (c2.id = r2.country_id)
group by c.id;

create unique index customer_addresses_view_idx on customer_addresses_view (customer_id);