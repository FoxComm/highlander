create or replace function update_gc_txn_insert_fn() returns trigger as $$
  begin
    insert into gift_card_transactions_view
      select
        -- Gift Card Transaction
        new.id,
        new.debit,
        new.credit,
        new.available_balance,
        new.state,
        to_json_timestamp(new.created_at) as created_at,
        -- Gift Card
        gc.code,
        gc.origin_type,
        gc.currency,
        to_json_timestamp(gc.created_at) as gift_card_created_at,
        -- Order Payment
        case when op is not null then
          to_json((
            o.reference_number,
            to_json_timestamp(o.placed_at),
            to_json_timestamp(op.created_at)
          )::export_order_payments)
        else null end as order_payment,
        -- Store admin
        case when sa is not null then
          to_json((u.email, u.name)::export_store_admins)
        else null end as store_admin,
        gc.scope
        from gift_cards as gc
          left join order_payments as op on (op.id = new.order_payment_id)
          left join orders as o on (op.cord_ref = o.reference_number)
          left join users as u on (u.account_id = new.store_admin_id)
          left join admin_data as sa on (sa.account_id = new.store_admin_id)
        where gc.id = new.gift_card_id;

    return null;
  end;
$$ language plpgsql;

create or replace function update_gc_txn_update_fn() returns trigger as $$
  begin
    update gift_card_transactions_view set
      debit = new.debit,
      credit = new.credit,
      available_balance = new.available_balance,
      state = new.state
    where id = new.id;
    return null;
  end;
$$ language plpgsql;

create or replace function update_gc_txn_view_from_order_payment_fn() returns trigger as $$
  begin
    update gift_card_transactions_view set
      order_payment = q.order_payment
      from (select
            new.id,
            case when op is not null then
              to_json((
                o.reference_number,
                to_json_timestamp(o.placed_at),
                to_json_timestamp(op.created_at)
              )::export_order_payments)
            else '{}' end as order_payment
        from order_payments as op
        inner join orders as o on (o.reference_number = op.cord_ref)
        where op.id = new.order_payment_id) as q
      where gift_card_transactions_view.id = q.id;

    return null;
  end;
$$ language plpgsql;


create trigger update_gc_txn_insert_fn
    after insert on gift_card_adjustments
    for each row
    execute procedure update_gc_txn_insert_fn();

create trigger update_gc_txn_update
    after update on gift_card_adjustments
    for each row
    execute procedure update_gc_txn_update_fn();

create trigger update_gc_txn_from_orders_payment
    after update on gift_card_adjustments
    for each row
    when (new.order_payment_id is not null and
      new.order_payment_id is distinct from old.order_payment_id)
    execute procedure update_gc_txn_view_from_order_payment_fn();
