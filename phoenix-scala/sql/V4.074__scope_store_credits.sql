-- Store credits table
alter table store_credits add column scope exts.ltree;
alter table store_credits_search_view add column scope exts.ltree;

update store_credits set scope = exts.text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

-- Store credits search view
update store_credits_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

create or replace function update_store_credits_view_insert_fn() returns trigger as $$
    begin
        insert into store_credits_search_view select distinct on (new.id)
            new.id as id,
            new.account_id as account_id,
            new.origin_id as origin_id,
            new.origin_type as origin_type,
            new.state as state,
            new.currency as currency,
            new.original_balance as original_balance,
            new.current_balance as current_balance,
            new.available_balance as available_balance,
            new.canceled_amount as canceled_amount,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
            to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
            '{}' as store_admin,
            new.scope as scope
            from store_credits as sc;
      return null;
  end;
$$ language plpgsql;

-- Store credits transactions view
drop materialized view store_credit_transactions_view;

create materialized view store_credit_transactions_view as
select distinct on (sca.id)
    -- Store Credit Transaction
    sca.id,
    sca.debit,
    sca.available_balance,
    sca.state,
    to_char(sca.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Store Credit
    sc.id as store_credit_id,
    sc.account_id,
    sc.origin_type,
    sc.currency,
    sc.scope,
    to_char(sc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as store_credit_created_at,
    -- Order Payment
    sctpv.order_payment,
    -- Store admins
    sctav.store_admin
from store_credit_adjustments as sca
inner join store_credits as sc on (sc.id = sca.store_credit_id)
inner join store_credit_transactions_payments_view as sctpv on (sctpv.id = sca.id)
inner join store_credit_transactions_admins_view as sctav on (sctav.id = sca.id)
order by sca.id;

create unique index store_credit_transactions_view_idx on store_credit_transactions_view (id);

alter table store_credits alter column scope set not null;
alter table store_credits_search_view alter column scope set not null;
