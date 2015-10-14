create table gift_cards (
    id integer primary key,
    origin_id integer not null,
    origin_type generic_string not null,
    subtype_id integer null references gift_card_subtypes(id) on update restrict on delete restrict,
    code generic_string not null unique,
    status generic_string not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    available_balance integer not null,
    reloadable boolean not null default false,
    canceled_amount integer null,
    canceled_reason integer null references reasons(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references payment_methods(id) on update restrict on delete restrict,
    foreign key (origin_id) references gift_card_origins(id) on update restrict on delete restrict,
    constraint valid_status check (status in ('onHold', 'active', 'canceled', 'cart', 'fullyRedeemed')),
    constraint positive_balance check (original_balance >= 0 and current_balance >= 0 and available_balance >= 0)
);

create index gift_cards_idx on gift_cards (code, status);

-- unique code generation prototype
create function generate_gift_card_code(len integer) returns text AS $$
declare
    new_code text;
    done bool;
begin
    done := false;
    while not done loop
        new_code := upper(substr(md5(random()::text), 0, len));
        done := not exists(select 1 from gift_cards WHERE code = new_code);
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

