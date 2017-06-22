create or replace function gift_card_transactions_view_insert_fn() returns trigger as $$
begin
  insert into gift_card_transactions_view 
    select distinct on (new.id)
        -- Gift Card Transaction
        gca.id as id,
        gca.debit as debit,
        gca.credit as credit,
        gca.available_balance as available_balance,
        gca.state as state,
        to_char(gca.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
        -- Gift Card
        gc.code as code,
        gc.origin_type as origin_type,
        gc.currency as currency,
        -- Order Payment
        case when count(op) = 0
          then '{}'
          else to_json((
                o.reference_number, 
                to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                to_char(op.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
            )::export_order_payments)
        end as order_payment,
        -- Store admins
        case when count(sa) = 0
          then '{}'
          else to_json((u.email, u.name)::export_store_admins)
        end as store_admin,
        gc.scope as scope
      from gift_card_adjustments as gca
        inner join gift_cards as gc on (gc.id = gca.gift_card_id)
        left join order_payments as op on (op.id = gca.order_payment_id)
        left join orders as o on (op.cord_ref = o.reference_number)
        left join admin_data as sa on (sa.account_id = gca.store_admin_id)
        left join users as u on (sa.account_id = sa.account_id)
      where gca.id = new.id
      group by gca.id, gc.id, op.id, o.id, sa.id, u.email, u.name;
  return null;
end;
$$ language plpgsql;

create or replace function update_gift_card_transactions_view_update_fn() returns trigger as $$
  begin
    update gift_card_transactions_view set
      debit = new.debit,
      available_balance = new.available_balance,
      state = new.state
    where id = new.id;
    return null;
  end;
$$ language plpgsql;

create or replace function update_gift_card_transactions_view_from_orders_update_fn() returns trigger as $$
  begin
    update gift_card_transactions_view set
      order_payment = q.order_payment
      from (select
            gca.id,
            case when count(op) = 0
              then '{}'
              else to_json((
                    o.reference_number, 
                    to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                    to_char(op.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
                )::export_order_payments)
            end as order_payment
            from gift_card_adjustments gca
            inner join order_payments op on op.id = gca.order_payment_id
            inner join orders o on o.reference_number = op.cord_ref
            where op.cord_ref = new.reference_number
            group by gca.id, o.reference_number, o.placed_at, op.created_at) as q
      where gift_card_transactions_view.id = q.id;
    return null;
  end;
$$ language plpgsql;

drop trigger if exists gift_card_transactions_view_insert on gift_card_adjustments;
drop trigger if exists update_gift_card_transactions_view_update on gift_card_adjustments;
drop trigger if exists update_gift_card_transactions_view_from_orders on orders;

create trigger gift_card_transactions_view_insert
  after insert on gift_card_adjustments
  for each row
  execute procedure gift_card_transactions_view_insert_fn();

create trigger update_gift_card_transactions_view_update
    after update on gift_card_adjustments
    for each row
    execute procedure update_gift_card_transactions_view_update_fn();

create trigger update_gift_card_transactions_view_from_orders
    after insert on orders
    for each row
    execute procedure update_gift_card_transactions_view_from_orders_update_fn();
