create materialized view order_payments_view as
select
    o.id as order_id,
    case when count(op) = 0
    then
        '[]'
    else
        json_agg((op.payment_method_type, op.amount, op.currency, ccp.state, gcc.state, sca.state)::export_payments)
    end as payments
from orders as o
left join order_payments as op on (o.reference_number = op.order_ref)
left join credit_card_charges as ccp on (op.id = ccp.order_payment_id)
left join gift_card_adjustments as gcc on (op.id = gcc.order_payment_id)
left join store_credit_adjustments as sca on (op.id = sca.order_payment_id)
group by o.id;

create unique index order_payments_view_idx on order_payments_view (order_id);