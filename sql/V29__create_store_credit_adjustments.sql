-- debit adjustments to store_credits
create table store_credit_adjustments (
    id serial,
    store_credit_id integer not null,
    debit integer not null default 0,
    capture boolean not null default false,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (store_credit_id) references store_credits(id) on update restrict on delete restrict,
    constraint valid_debit check (debit > 0)
);

create function update_store_credit_current_balance() returns trigger as $$
begin
    update store_credits set current_balance = current_balance - new.debit where id = new.store_credit_id;
    return new;
end;
$$ language plpgsql;

create trigger update_store_credit_current_balance
    after insert
    on store_credit_adjustments
    for each row
    execute procedure update_store_credit_current_balance();

