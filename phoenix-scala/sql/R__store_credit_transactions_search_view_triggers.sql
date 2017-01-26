create or replace function update_store_credit_transactions_view_insert_fn() returns trigger as $$
  begin
    insert into store_credit_transactions_search_view select distinct on (new.id)
      sca.id,
      sc.account_id as account_id,
      sc.id as store_credit_id,
      to_json_timestamp(sca.created_at) as created_at,
      to_json_timestamp(sc.created_at) as store_credit_created_at,
      sca.debit,
      sca.available_balance,
      sca.state,
      sc.origin_type,
      sc.currency,

      -- Order payment
      case when count(op) = 0 then '{}'
        else to_json((
          o.reference_number,
          to_json_timestamp(o.placed_at),
          to_json_timestamp(op.created_at)
        )::export_order_payments)::jsonb
        end as order_payment,

      -- Store admin
      case when count(sa) = 0 then '{}'
        else to_json((u.email, u.name)::export_store_admins)::jsonb
        end as store_admin,
      sc.scope
    from store_credit_adjustments as sca
    inner join store_credits as sc on (sc.id = sca.store_credit_id)
    inner join order_payments as op on (op.id = sca.order_payment_id)
    left join orders as o on (op.cord_ref = o.reference_number)
    left join admin_data as sa on (sa.account_id = sca.store_admin_id)
    left join users as u on (sa.account_id = sa.account_id)
    where sca.id = new.id
    group by sca.id, sc.id, o.reference_number, o.placed_at, op.created_at, u.email, u.name;

    return null;
  end;
$$ language plpgsql;

create or replace function update_store_credit_transactions_view_update_fn() returns trigger as $$
  begin
    update store_credit_transactions_search_view set
      debit = new.debit,
      available_balance = new.available_balance,
      state = new.state
    where id = new.id;
    return null;
  end;
$$ language plpgsql;

create or replace function update_store_credit_transactions_view_from_orders_update_fn() returns trigger as $$
  begin
    update store_credit_transactions_search_view set
      order_payment = q.order_payment
      from (select
            sca.id,
            case when count(op) = 0 then '{}'
            else to_json((
              o.reference_number,
              to_json_timestamp(o.placed_at),
              to_json_timestamp(op.created_at)
            )::export_order_payments)::jsonb
            end as order_payment
        from store_credit_adjustments sca
        inner join order_payments op on op.id = sca.order_payment_id
        inner join orders o on o.reference_number = op.cord_ref
        where op.cord_ref = new.reference_number
        group by sca.id, o.reference_number, o.placed_at, op.created_at) as q
      where store_credit_transactions_search_view.id = q.id;

    return null;
  end;
$$ language plpgsql;

drop trigger if exists update_store_credit_transactions_view_insert on store_credit_adjustments;
drop trigger if exists update_store_credit_transactions_view_update on store_credit_adjustments;
drop trigger if exists update_store_credit_transactions_view_from_orders on orders;

create trigger update_store_credit_transactions_view_insert
    after insert on store_credit_adjustments
    for each row
    execute procedure update_store_credit_transactions_view_insert_fn();

create trigger update_store_credit_transactions_view_update
    after update on store_credit_adjustments
    for each row
    execute procedure update_store_credit_transactions_view_update_fn();

create trigger update_store_credit_transactions_view_from_orders
    after insert on orders
    for each row
    execute procedure update_store_credit_transactions_view_from_orders_update_fn();
