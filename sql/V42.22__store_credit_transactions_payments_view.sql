create materialized view store_credit_transactions_payments_view as
select
    sca.id,
    -- Order Payments
    case when count(op) = 0
    then
        null
    else
        to_json((
            o.reference_number, 
            to_char(o.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'), 
            to_char(op.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
        )::export_order_payments)
    end as order_payment  
from store_credit_adjustments as sca
inner join store_credits as sc on (sca.store_credit_id = sc.id)
left join order_payments as op on (op.id = sca.order_payment_id)
left join orders as o on (op.order_ref = o.reference_number)
group by sca.id, op.id, o.id;

create unique index store_credit_transactions_payments_view_idx on store_credit_transactions_payments_view (id);
