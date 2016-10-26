-- Gift cards table
alter table gift_cards add column scope exts.ltree;
alter table gift_cards_search_view add column scope exts.ltree;

update gift_cards set scope = exts.text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

-- Gift cards search view
update gift_cards_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

create or replace function update_gift_cards_view_insert_fn() returns trigger as $$
    begin
        insert into gift_cards_search_view select distinct on (new.id)
            new.id as id,
            new.origin_type as origin_type,
            new.code as code,
            new.state as state,
            new.currency as currency,
            new.original_balance as original_balance,
            new.current_balance as current_balance,
            new.available_balance as available_balance,
            new.canceled_amount as canceled_amount,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
            to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
            new.scope as scope
            from gift_cards as gc;
      return null;
  end;
$$ language plpgsql;

-- Gift cards transactions view
drop materialized view gift_card_transactions_view;

create materialized view gift_card_transactions_view as
select distinct on (gca.id)
    -- Gift Card Transaction
    gca.id,
    gca.debit,
    gca.credit,
    gca.available_balance,
    gca.state,
    to_char(gca.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Gift Card
    gc.code,
    gc.origin_type,
    gc.currency,
    gc.scope,
    to_char(gc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as gift_card_created_at,
    -- Order Payment
    gctpv.order_payment,
    -- Store admins
    gctav.store_admin
from gift_card_adjustments as gca
inner join gift_cards as gc on (gc.id = gca.gift_card_id)
inner join gift_card_transactions_payments_view as gctpv on (gctpv.id = gca.id)
inner join gift_card_transactions_admins_view as gctav on (gctav.id = gca.id)
order by gca.id;

create unique index gift_card_transactions_view_idx on gift_card_transactions_view (id);

alter table gift_cards alter column scope set not null;
alter table gift_cards_search_view alter column scope set not null;
