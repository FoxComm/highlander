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
            gca.id,
            case when op is not null 
              then to_json((
                    o.reference_number, 
                    to_json_timestamp(o.placed_at),
                    to_json_timestamp(op.created_at)
                )::export_order_payments)
              else null
            end as order_payment
            from gift_card_adjustments gca
            inner join order_payments op on op.id = gca.order_payment_id
            inner join orders o on o.reference_number = op.cord_ref
            where op.cord_ref = new.reference_number
            group by gca.id, o.reference_number, o.placed_at, op.id) as q
      where gift_card_transactions_view.id = q.id;
    return null;
  end;
$$ language plpgsql;

drop trigger if exists update_gc_txn_insert_fn on gift_card_adjustments;
drop trigger if exists update_gc_txn_update on gift_card_adjustments;
drop trigger if exists update_gc_txn_from_orders_payment on orders;

create trigger update_gc_txn_insert_fn
    after insert on gift_card_adjustments
    for each row
    execute procedure update_gc_txn_insert_fn();

create trigger update_gc_txn_update
    after update on gift_card_adjustments
    for each row
    execute procedure update_gc_txn_update_fn();

create trigger update_gc_txn_from_orders_payment
    after update on orders
    for each row
    execute procedure update_gc_txn_view_from_order_payment_fn();
