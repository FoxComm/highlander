create materialized view order_billing_addresses_view as
select
    o.id as order_id,
    count(cc) as count,
    case when count(cc) = 0
    then
        '[]'
    else
        json_agg((cc.address1, cc.address2, cc.city, cc.zip, r2.name, c2.name, c2.continent, c2.currency)::export_addresses)
    end as addresses 
from orders as o
left join order_payments as op_cc on (o.id = op_cc.order_id and op_cc.payment_method_type = 'creditCard')
left join credit_cards as cc on (cc.id = op_cc.payment_method_id)
left join regions as r2 on (cc.region_id = r2.id)
left join countries as c2 on (r2.country_id = c2.id)
group by o.id;

create unique index order_billing_addresses_view_idx on order_billing_addresses_view (order_id);