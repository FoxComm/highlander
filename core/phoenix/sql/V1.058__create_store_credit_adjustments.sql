-- debit adjustments to store_credits
create table store_credit_adjustments (
    id serial primary key,
    store_credit_id integer not null,
    order_payment_id integer null,
    store_admin_id integer null,
    debit integer not null,
    available_balance integer not null default 0,
    state character varying(255) not null,
    created_at generic_timestamp,
    foreign key (store_credit_id) references store_credits(id) on update restrict on delete restrict,
    foreign key (order_payment_id) references order_payments(id) on update restrict on delete restrict,
    constraint valid_debit check (
        (debit > 0 and state <> 'cancellationCapture') or 
        (debit >= 0 and state = 'cancellationCapture')
    ),
    constraint valid_state check (state in ('auth','canceled','capture','cancellationCapture'))
);

create function update_store_credit_current_balance() returns trigger as $$
declare
    adjustment integer default 0;
    new_available_balance integer default 0;
begin
    adjustment = new.debit;

    -- canceling an adjustment should remove its monetary change from the gc
    if new.state = 'canceled' and old.state != 'canceled' then
        if old.state = 'capture' or old.state = 'cancellationCapture' then
            update store_credits
                set current_balance = current_balance + adjustment,
                    available_balance = available_balance + adjustment
                where id = new.store_credit_id
                returning available_balance into new_available_balance;
        else
            update store_credits
                set available_balance = available_balance + adjustment
                where id = new.store_credit_id
                returning available_balance into new_available_balance;
        end if;

        new.available_balance = new_available_balance;
        return new;
    end if;

    -- handle credit or debit for auth or capture
    if new.state = 'capture' then
        update store_credits
            set current_balance = current_balance - adjustment,
                available_balance = available_balance - adjustment
            where id = new.store_credit_id
            returning available_balance into new_available_balance;
    else
        update store_credits
            set available_balance = available_balance - adjustment
            where id = new.store_credit_id
            returning available_balance into new_available_balance;
    end if;

    new.available_balance = new_available_balance;
    return new;
end;
$$ language plpgsql;

create trigger set_store_credit_current_balance_trg
    before insert
    on store_credit_adjustments
    for each row
    execute procedure update_store_credit_current_balance();

create trigger update_store_credit_current_balance_trg
    before update
    on store_credit_adjustments
    for each row
    execute procedure update_store_credit_current_balance();

create index store_credit_adjustments_payment_state_idx on store_credit_adjustments (order_payment_id, state);
create index store_credit_adjustments_store_credit_idx on store_credit_adjustments (store_credit_id);

