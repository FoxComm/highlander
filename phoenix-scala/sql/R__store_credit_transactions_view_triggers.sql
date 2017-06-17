-- store_credit_transactions_payments_view

create or replace function update_sc_txn_payments_view_fn() returns trigger as $$
begin
  insert into store_credit_transactions_payments_view
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
    from store_credits as sc
    left join order_payments as op on (op.id = new.order_payment_id)
    left join orders as o on (op.cord_ref = o.reference_number)
    where new.store_credit_id = sc.id
    group by new.id, op.id, o.id
    on conflict (id)
        do update set order_payment = excluded.order_payment;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_sc_txn_payments_view on store_credit_adjustments;

create trigger update_sc_txn_payments_view
    after insert or update on store_credit_adjustments
    for each row
    execute procedure update_sc_txn_payments_view_fn();