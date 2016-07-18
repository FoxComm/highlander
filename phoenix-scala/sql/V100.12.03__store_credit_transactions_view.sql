create materialized view store_credit_transactions_view as
select distinct on (sca.id)
    -- Store Credit Transaction
    sca.id,
    sca.debit,
    sca.available_balance,
    sca.state,
    to_char(sca.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Store Credit
    sc.id as store_credit_id,
    sc.customer_id,
    sc.origin_type,
    sc.currency,
    to_char(sc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as store_credit_created_at,
    -- Order Payment
    sctpv.order_payment,
    -- Store admins
    sctav.store_admin
from store_credit_adjustments as sca
inner join store_credits as sc on (sc.id = sca.store_credit_id)
inner join store_credit_transactions_payments_view as sctpv on (sctpv.id = sca.id)
inner join store_credit_transactions_admins_view as sctav on (sctav.id = sca.id)
order by sca.id;

create unique index store_credit_transactions_view_idx on store_credit_transactions_view (id);
