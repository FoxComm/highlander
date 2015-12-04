create materialized view order_credit_card_payments_view as
select
    o.id as order_id,
    count(op.id) as count,
    coalesce(sum(op.amount), 0) as total
from orders as o
left join order_payments as op on (o.id = op.order_id and op.payment_method_type = 'creditCard')
group by o.id;

create unique index order_credit_card_payments_view_idx on order_credit_card_payments_view (order_id);