create or replace function update_store_credit_current_balance() returns trigger as $$
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
            set current_balance = current_balance - adjustment
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
