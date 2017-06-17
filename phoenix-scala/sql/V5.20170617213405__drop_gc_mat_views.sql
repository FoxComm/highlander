drop materialized view gift_card_transactions_payments_view;

create table gift_card_transactions_payments_view(id integer, order_payment jsonb);

create unique index gift_card_transactions_payments_view_idx on gift_card_transactions_payments_view (id);

insert into gift_card_transactions_payments_view
    select
        gca.id,
        -- Order Payments
        case when count(op) = 0
        then
            null
        else
            to_json((
                o.reference_number,
                to_json_timestamp(o.placed_at),
                to_json_timestamp(op.created_at)
            )::export_order_payments)
        end as order_payment
    from gift_card_adjustments as gca
    inner join gift_cards as gc on (gca.gift_card_id = gc.id)
    left join order_payments as op on (op.id = gca.order_payment_id)
    left join orders as o on (op.cord_ref = o.reference_number)
    group by gca.id, op.id, o.id;