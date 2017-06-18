-- store_credit_transactions_payments_view

create or replace function update_sc_txn_payments_view_fn() returns trigger as $$
begin
  insert into store_credit_transactions_payments_view
    select
      new.id,
      -- Order Payments
      case when op is not null then
        to_json((
          o.reference_number,
          to_json_timestamp(o.placed_at),
          to_json_timestamp(op.created_at)
        )::export_order_payments)
      else null end,
      o.scope
    from store_credits as sc
    left join order_payments as op on (op.id = new.order_payment_id)
    left join orders as o on (op.cord_ref = o.reference_number)
    where new.store_credit_id = sc.id
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

-- store_credit_transactions_admins_view

create or replace function update_sc_txn_admins_view_fn() returns trigger as $$
begin
  insert into store_credit_transactions_admins_view
    select
      sca.id,
      -- Store admins
      case when sa is not null then
        to_json((u.email, u.name)::export_store_admins)
      else null end,
      sa.scope
    from store_credit_adjustments as sca
    inner join store_credits as sc on (sc.id = sca.store_credit_id)
    left join admin_data as sa on (sa.account_id = sca.store_admin_id)
    left join users as u on (u.account_id = sa.account_id)
  on conflict (id)
    do update set store_admin = excluded.store_admin;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_sc_txn_admins_view on store_credit_adjustments;

create trigger update_sc_txn_admins_view
    after insert or update on store_credit_adjustments
    for each row
    execute procedure update_sc_txn_admins_view_fn();
