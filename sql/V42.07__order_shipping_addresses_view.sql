create materialized view order_shipping_addresses_view as
select
    o.id as order_id,
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
group by o.id;

create unique index order_shipping_addresses_view_idx on order_shipping_addresses_view (order_id);