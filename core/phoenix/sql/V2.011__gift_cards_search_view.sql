drop materialized view gift_cards_search_view;

create table gift_cards_search_view
(
    id bigint not null unique,
    origin_type gift_card_origin_type,
    code generic_string not null,    
    state gift_card_state not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    available_balance integer not null,
    canceled_amount integer null,
    created_at text,
    updated_at text
);

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
            to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at
            from gift_cards as gc;
      return null;
  end;
$$ language plpgsql;

create or replace function update_gift_cards_view_update_fn() returns trigger as $$
begin
    update gift_cards_search_view set
        origin_type = new.origin_type,
        code = new.code,
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

create trigger update_gift_cards_view_insert
    after insert on gift_cards
    for each row
    execute procedure update_gift_cards_view_insert_fn();

create trigger update_gift_cards_view_update
    after update on gift_cards
    for each row
    execute procedure update_gift_cards_view_update_fn();
