create materialized view store_credit_transactions_view as
select distinct on (sca.id)
    -- Store Credit Transaction
    sca.id,
    sca.debit,
    sca.available_balance,
    sca.state,
    to_char(sca.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at,
    -- Store Credit
    sc.id as store_credit_id,
    sc.customer_id,
    sc.origin_type,
    sc.currency,
    to_char(sc.created_at, 'YYYY-MM-DD HH24:MI:SS') as store_credit_created_at,
    -- Order
    o.reference_number as order_reference_number,
    to_char(o.created_at, 'YYYY-MM-DD HH24:MI:SS') as order_created_at,
    to_char(op.created_at, 'YYYY-MM-DD HH24:MI:SS') as order_payment_created_at,
    -- Store admins
    sctav.store_admin
from store_credit_adjustments as sca
inner join store_credits as sc on (sc.id = sca.store_credit_id)
inner join order_payments as op on (op.id = sca.order_payment_id)
inner join orders as o on (op.order_id = o.id)
inner join store_credit_transactions_admins_view as sctav on (sctav.id = sca.id)
order by sca.id;

create unique index store_credit_transactions_view_idx on store_credit_transactions_view (id);
