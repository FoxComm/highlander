create materialized view gift_card_transactions_payments_view as
select
    gca.id,
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
from gift_card_adjustments as gca
inner join gift_cards as gc on (gca.gift_card_id = gc.id)
left join order_payments as op on (op.id = gca.order_payment_id)
left join orders as o on (op.order_id = o.id)
group by gca.id, op.id, o.id;

create unique index gift_card_transactions_payments_view_idx on gift_card_transactions_payments_view (id);
