create materialized view gift_card_transactions_view as
select distinct on (gca.id)
    -- Gift Card Transaction
    gca.id,
    gca.debit,
    gca.credit,
    gca.available_balance,
    gca.state,
    to_char(gca.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Gift Card
    gc.code,
    gc.origin_type,
    gc.currency,
    to_char(gc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as gift_card_created_at,
    -- Order Payment
    gctpv.order_payment,
    -- Store admins
    gctav.store_admin
from gift_card_adjustments as gca
inner join gift_cards as gc on (gc.id = gca.gift_card_id)
inner join gift_card_transactions_payments_view as gctpv on (gctpv.id = gca.id)
inner join gift_card_transactions_admins_view as gctav on (gctav.id = gca.id)
order by gca.id;

create unique index gift_card_transactions_view_idx on gift_card_transactions_view (id);
