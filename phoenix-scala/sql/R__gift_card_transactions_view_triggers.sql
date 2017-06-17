--  gift_card_transactions_payments_view

create or replace function update_gc_txn_payments_view_fn() returns trigger as $$
begin
  insert into gift_card_transactions_payments_view
      select
            new.id,
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
        from gift_cards as gc
        left join order_payments as op on (op.id = new.order_payment_id)
        left join orders as o on (op.cord_ref = o.reference_number)
        where new.gift_card_id = gc.id
        group by new.id, op.id, o.id
    on conflict (id)
        do update set order_payment = excluded.order_payment;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_gc_txn_payments_view_fn on gift_card_adjustments;

create trigger update_gc_txn_payments_view
    after insert or update on gift_card_adjustments
    for each row
    execute procedure update_gc_txn_payments_view_fn();

-- gift_card_transactions_admins_view

create or replace function update_gc_txn_admins_view_fn() returns trigger as $$
begin
  insert into gift_card_transactions_admins_view
      select
            new.id,
            -- Store admins
            case when count(sa) = 0
            then
                null
            else
                to_json((u.email, u.name)::export_store_admins)
            end as store_admin
        from gift_cards as gc
        left join users as u on (u.account_id = new.store_admin_id)
        left join admin_data as sa on (sa.account_id = new.store_admin_id)
    where gc.id = new.gift_card_id
    group by new.id, sa.account_id, u.email, u.name
    on conflict (id)
        do update set store_admin = excluded.store_admin;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_gc_txn_admins_view on gift_card_adjustments;

create trigger update_gc_txn_admins_view
    after insert or update on gift_card_adjustments
    for each row
    execute procedure update_gc_txn_admins_view_fn();