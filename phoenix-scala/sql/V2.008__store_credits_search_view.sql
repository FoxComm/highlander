drop materialized view store_credits_search_view;

create table store_credits_search_view
(
    -- Store credit
    id bigint not null unique,
    customer_id integer not null,
    origin_id integer not null,
    origin_type store_credit_origin_type,
    state store_credit_state not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    available_balance integer not null,
    canceled_amount integer null,
    created_at text,
    updated_at text,
    store_admin jsonb not null default '{}'
);

create or replace function update_store_credits_view_insert_fn() returns trigger as $$
    begin
        insert into store_credits_search_view select distinct on (new.id)
            new.id as id,
            new.customer_id as customer_id,
            new.origin_id as origin_id,
            new.origin_type as origin_type,           
            new.state as state,
            new.currency as currency,
            new.original_balance as original_balance,
            new.current_balance as current_balance,
            new.available_balance as available_balance,
            new.canceled_amount as canceled_amount,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
            to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at
            from store_credits as sc;
      return null;
  end;
$$ language plpgsql;

create or replace function update_store_credits_view_update_fn() returns trigger as $$
begin
    update store_credits_search_view set
        customer_id = new.customer_id,
        origin_id = new.origin_id,
        origin_type = new.origin_type,
        state = new.state,
        currency = new.currency,
        original_balance = new.original_balance,
        available_balance = new.available_balance,
        current_balance = new.current_balance,
        canceled_amount = new.canceled_amount,
        created_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        updated_at = to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where id = new.id;
    return null;
    end;
$$ language plpgsql;

create trigger update_store_credits_view_insert
    after insert on store_credits
    for each row
    execute procedure update_store_credits_view_insert_fn();

create trigger update_store_credits_view_update
    after update on store_credits
    for each row
    execute procedure update_store_credits_view_update_fn();
