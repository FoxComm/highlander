create materialized view customer_shipping_addresses_view as
select
    c.id as customer_id,
    count(a) as count,
    case when count(a) = 0
    then
        '[]'
    else
        json_agg((a.address1, a.address2, a.city, a.zip, r1.name, c1.name, c1.continent, c1.currency)::export_addresses)
    end as addresses
from customers as c
left join addresses as a on (c.id = a.customer_id)
left join regions as r1 on (r1.id = a.region_id)
left join countries as c1 on (c1.id = r1.country_id)
group by c.id;

create unique index customer_shipping_addresses_view_idx on customer_shipping_addresses_view (customer_id);