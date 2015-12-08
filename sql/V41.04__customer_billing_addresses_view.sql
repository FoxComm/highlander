create materialized view customer_billing_addresses_view as
select
    c.id as customer_id,
    count(cc) as count,
    case when count(cc) = 0
    then
        '[]'
    else
        json_agg((cc.address1, cc.address2, cc.city, cc.zip, r2.name, c2.name, c2.continent, c2.currency)::export_addresses)
    end as addresses
from customers as c
left join credit_cards as cc on (c.id = cc.customer_id)
left join regions as r2 on (r2.id = cc.region_id)
left join countries as c2 on (c2.id = r2.country_id)
group by c.id;

create unique index customer_billing_addresses_view_idx on customer_billing_addresses_view (customer_id);