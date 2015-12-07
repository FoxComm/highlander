create materialized view order_addresses_view as
select
    o.id as order_id,
    -- Shipping addresses
    case when count(osa) = 0
    then
        '[]'
    else
        json_agg((osa.address1, osa.address2, osa.city, osa.zip, r1.name, c1.name, c1.continent, c1.currency)::export_addresses)
    end as shipping_addresses,
    -- Billing addresses
    case when count(cc) = 0
    then
        '[]'
    else
        json_agg((cc.address1, cc.address2, cc.city, cc.zip, r2.name, c2.name, c2.continent, c2.currency)::export_addresses)
    end as billing_addresses 
from orders as o
-- Shipping addresses
left join order_shipping_addresses as osa on (o.id = osa.order_id)
left join regions as r1 on (osa.region_id = r1.id)
left join countries as c1 on (r1.country_id = c1.id)
-- Billing addresses
left join order_payments as op on (o.id = op.order_id)
left join order_payments as op_cc on (o.id = op.order_id and op.payment_method_type = 'creditCard')
left join credit_cards as cc on (cc.id = op_cc.payment_method_id)
left join regions as r2 on (osa.region_id = r2.id)
left join countries as c2 on (r2.country_id = c2.id)
group by o.id;

create unique index order_addresses_view_idx on order_addresses_view (order_id);