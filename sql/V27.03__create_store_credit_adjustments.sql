-- debit adjustments to store_credits
create table store_credit_adjustments (
    id serial primary key,
    store_credit_id integer not null,
    order_payment_id integer null,
    store_admin_id integer null,
    debit integer not null,
    status character varying(255) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (store_credit_id) references store_credits(id) on update restrict on delete restrict,
    foreign key (order_payment_id) references order_payments(id) on update restrict on delete restrict,
    constraint valid_debit check (debit > 0),
    constraint valid_status check (status in ('auth','canceled','capture'))
);

create function update_store_credit_current_balance() returns trigger as $$
declare
    adjustment integer default 0;
begin
    adjustment = new.debit;

    -- canceling an adjustment should remove its monetary change from the gc
    if new.status = 'canceled' and old.status != 'canceled' then
        if old.status = 'capture' then
            update store_credits
                set current_balance = current_balance + adjustment,
                    available_balance = available_balance + adjustment
                    where id = new.store_credit_id;
            return new;
        else
            update store_credits set available_balance = available_balance + adjustment where id = new.store_credit_id;
            return new;
        end if;
    end if;

    -- handle credit or debit for auth or capture
    if new.status = 'capture' then
        update store_credits
            set current_balance = current_balance - adjustment,
                available_balance = available_balance - adjustment
            where id = new.store_credit_id;
    else
        update store_credits set available_balance = available_balance - adjustment where id = new.store_credit_id;
    end if;

    return new;
end;
$$ language plpgsql;

create trigger set_store_credit_current_balance_trg
    after insert
    on store_credit_adjustments
    for each row
    execute procedure update_store_credit_current_balance();

create trigger update_store_credit_current_balance_trg
    after update
    on store_credit_adjustments
    for each row
    execute procedure update_store_credit_current_balance();

create index store_credit_adjustments_payment_status_idx on store_credit_adjustments (order_payment_id, status);
create index store_credit_adjustments_store_credit_idx on store_credit_adjustments (store_credit_id);

