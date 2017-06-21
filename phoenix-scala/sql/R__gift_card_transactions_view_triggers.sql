create or replace function gift_card_transactions_view_insert_fn() returns trigger as $$
begin
  REFRESH MATERIALIZED VIEW gift_card_transactions_payments_view;
  REFRESH MATERIALIZED VIEW gift_card_transactions_admins_view;
  insert into gift_card_transactions_view(
    id,
    debit,
    credit,
    available_balance,
    state,
    created_at,
    code,
    origin_type,
    currency,
    scope,
    order_payment,
    store_admin) 
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
        gc.scope as scope,
        -- Order Payment
        gctpv.order_payment as order_payment,
        -- Store admins
        gctav.store_admin as store_admin
      from gift_card_adjustments as gca
        inner join gift_cards as gc on (gc.id = gca.gift_card_id)
        inner join gift_card_transactions_payments_view as gctpv on (gctpv.id = gca.id)
        inner join gift_card_transactions_admins_view as gctav on (gctav.id = gca.id)
      where new.id = gca.id;
  return null;
end;
$$ language plpgsql;

drop trigger if exists gift_card_transactions_view_insert on gift_card_adjustments;
create trigger gift_card_transactions_view_insert
  after insert on gift_card_adjustments
  for each row
  execute procedure gift_card_transactions_view_insert_fn();
