create table gift_cards (
    id integer primary key references payment_methods(id) on update restrict on delete restrict,
    origin_id integer not null references gift_card_origins(id) on update restrict on delete restrict,
    origin_type gift_card_origin_type,
    subtype_id integer null references gift_card_subtypes(id) on update restrict on delete restrict,
    code generic_string not null unique,
    state gift_card_state not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    available_balance integer not null,
    reloadable boolean not null default false,
    customer_id integer null references customers(id) on update restrict on delete restrict,
    canceled_amount integer null,
    canceled_reason integer null references reasons(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    constraint positive_balance check (original_balance >= 0 and current_balance >= 0 and available_balance >= 0)
);

create index gift_cards_idx on gift_cards (code, state);

-- unique code generation prototype
create function generate_gift_card_code(len integer) returns text as $$
declare
    new_code text;
    done bool;
begin
    done := false;
    while not done loop
        new_code := upper(substr(md5(random()::text), 0, len + 1));
        done := not exists(select 1 from gift_cards where code = new_code);
    end loop;
    return new_code;
end;
$$ language plpgsql;

create function set_gift_cards_codes() returns trigger as $$
begin
    new.code = generate_gift_card_code(16);
    return new;
end;
$$ language plpgsql;

create trigger set_gift_card_code_trg
    before insert
    on gift_cards
    for each row
    execute procedure set_gift_cards_codes();

-- available_balance and current_balance should always be == original_balance upon insertion
create function set_gift_cards_balances() returns trigger as $$
begin
    new.current_balance = new.original_balance;
    new.available_balance = new.original_balance;
    return new;
end;
$$ language plpgsql;

create trigger set_payment_method_id_trg
    before insert
    on gift_cards
    for each row
    execute procedure set_payment_method_id();

create trigger set_gift_cards_balances_trg
    before insert
    on gift_cards
    for each row
    execute procedure set_gift_cards_balances();
