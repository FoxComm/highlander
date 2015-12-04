create materialized view customer_orders_view as
select
    c.id as customer_id,
    count(o1.id) as count,
    case when count(o1) = 0
    then
        '[]'
    else
        json_agg((o1.reference_number, o1.status, to_char(o1.created_at, 'YYYY-MM-dd'), to_char(o1.placed_at, 'YYYY-MM-dd'))::export_orders)
    end as orders
from customers as c
left join orders as o1 on c.id = o1.customer_id
group by c.id;

create unique index customer_orders_view_idx on customer_orders_view (customer_id);