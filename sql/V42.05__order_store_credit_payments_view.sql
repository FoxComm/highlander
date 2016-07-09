create materialized view order_store_credit_payments_view as
select
    o.id as order_id,
    count(op.id) as count,
    coalesce(sum(op.amount), 0) as total
from orders as o
left join order_payments as op on (o.reference_number = op.cord_ref and op.payment_method_type = 'storeCredit')
group by o.id;

create unique index order_store_credit_payments_view_idx on order_store_credit_payments_view (order_id);