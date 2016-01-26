create materialized view gift_card_transactions_view as
select distinct on (gca.id)
    -- Gift Card Transaction
    gca.id,
    gca.debit,
    gca.credit,
    gca.available_balance,
    gca.status,
    to_char(gca.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at,
    -- Gift Card
    gc.code,
    gc.origin_type,
    gc.currency,
    to_char(gc.created_at, 'YYYY-MM-DD HH24:MI:SS') as store_credit_created_at,
    -- Order
    o.reference_number as order_reference_number,
    to_char(o.created_at, 'YYYY-MM-DD HH24:MI:SS') as order_created_at,
    to_char(op.created_at, 'YYYY-MM-DD HH24:MI:SS') as order_payment_created_at,
    -- Store admins
    gctav.store_admin
from gift_card_adjustments as gca
inner join gift_cards as gc on (gc.id = gca.gift_card_id)
inner join order_payments as op on (op.id = gca.order_payment_id)
inner join orders as o on (op.order_id = o.id)
inner join gift_card_transactions_admins_view as gctav on (gctav.id = gca.id)
order by gca.id;

create unique index gift_card_transactions_view_idx on gift_card_transactions_view (id);
