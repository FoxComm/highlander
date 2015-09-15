create table store_credits (
    id integer primary key,
    customer_id integer not null,
    origin_id integer not null,
    origin_type character varying(255) not null,
    status character varying(255) not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    available_balance integer not null,
    canceled_amount integer null,
    canceled_reason character varying(255) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references payment_methods(id) on update restrict on delete restrict,
    foreign key (origin_id) references store_credit_origins(id) on update restrict on delete restrict,
    constraint valid_status check (status in ('onHold', 'active', 'canceled')),
    constraint positive_balance check (original_balance >= 0 and current_balance >= 0 and available_balance >= 0)
);

create index store_credits_idx on store_credits (customer_id, status);

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

