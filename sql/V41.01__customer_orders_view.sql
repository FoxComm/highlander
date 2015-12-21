create materialized view customer_orders_view as
select
    c.id as customer_id,
    count(o1.id) as count,
    case when count(o1) = 0
    then
        '[]'
    else
        json_agg((
        	o1.reference_number,
        	o1.status,
        	to_char(o1.created_at, 'YYYY-MM-DD HH24:MI:SS'),
        	to_char(o1.placed_at, 'YYYY-MM-DD HH24:MI:SS'),
        	o1.sub_total,
        	o1.shipping_total,
        	o1.adjustments_total,
        	o1.taxes_total,
        	o1.grand_total
        )::export_orders)
    end as orders
from customers as c
left join orders as o1 on c.id = o1.customer_id
group by c.id;

create unique index customer_orders_view_idx on customer_orders_view (customer_id);
