create materialized view order_payments_view as
select
    o.id as order_id,
    case when count(op) = 0
    then
        '[]'
    else
        json_agg((op.payment_method_type, op.amount, op.currency)::export_payments)
    end as payments
from orders as o
left join order_payments as op on (o.id = op.order_id)
group by o.id;

create unique index order_payments_view_idx on order_payments_view (order_id);