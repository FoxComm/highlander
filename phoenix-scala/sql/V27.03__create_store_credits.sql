create table store_credits (
    id integer primary key references payment_methods(id) on update restrict on delete restrict,
    customer_id integer not null,
    origin_id integer not null references store_credit_origins(id) on update restrict on delete restrict,
    origin_type store_credit_origin_type,
    subtype_id integer null references store_credit_subtypes(id) on update restrict on delete restrict,
    state store_credit_state not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    available_balance integer not null,
    canceled_amount integer null,
    canceled_reason integer null references reasons(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    constraint positive_balance check (original_balance >= 0 and current_balance >= 0 and available_balance >= 0)
);

create index store_credits_idx on store_credits (customer_id, state);

-- available_balance and current_balance should always be == original_balance upon insertion
create function set_store_credits_balances() returns trigger as $$
begin
    new.current_balance = new.original_balance;
    new.available_balance = new.original_balance;
    return new;
end;
$$ language plpgsql;

create trigger set_payment_method_id_trg
    before insert
    on store_credits
    for each row
    execute procedure set_payment_method_id();

create trigger set_store_credits_balances_trg
    before insert
    on store_credits
    for each row
    execute procedure set_store_credits_balances();

