create or replace function update_gift_card_current_balance() returns trigger as $$
declare
    adjustment integer default 0;
    new_available_balance integer default 0;
    refund integer default 0;
begin
    if new.debit > 0 then
        adjustment := new.debit;
    elsif new.credit > 0 then
        adjustment := -new.credit;
    end if;

    if new.state = 'auth' then
            update gift_cards
                set available_balance = available_balance - adjustment
                where id = new.gift_card_id
                returning available_balance into new_available_balance;
        new.available_balance = new_available_balance;
    elsif new.state = 'canceled' then
            update gift_cards
                set available_balance = available_balance + adjustment
                where id = new.gift_card_id
                returning available_balance into new_available_balance;
        new.available_balance = new_available_balance;
    elsif new.state = 'capture' then
        if tg_op = 'INSERT' then
            refund := -adjustment;
        elsif old.state = 'auth'  then
            refund := old.debit - new.debit;
        end if;
        update gift_cards
            set current_balance = current_balance - adjustment,
            available_balance = available_balance + refund
            where id = new.gift_card_id;
    elsif new.state = 'cancellationCapture' then
            update gift_cards
                set current_balance = current_balance + adjustment,
                available_balance = available_balance + adjustment
                where id = new.gift_card_id;
    else
        raise exception 'Failed to update gift card balance with id %, the state % is unknown', new.gift_card_id, new.state;
    end if;
    return new;
end;
$$ language plpgsql;

create or replace function update_store_credit_current_balance() returns trigger as $$
declare
    adjustment integer default 0;
    new_available_balance integer default 0;
    refund integer default 0;
begin
    adjustment := new.debit;

    if new.state = 'auth' then
        update store_credits
            set available_balance = available_balance - adjustment
            where id = new.store_credit_id
            returning available_balance into new_available_balance;
        new.available_balance = new_available_balance;
    elsif new.state = 'canceled' then
        update store_credits
            set available_balance = available_balance + adjustment
            where id = new.store_credit_id
            returning available_balance into new_available_balance;
        new.available_balance = new_available_balance;
    elsif new.state = 'capture' then
        if tg_op = 'INSERT' then
            refund := -adjustment;
        elsif old.state = 'auth'  then
            refund := old.debit - new.debit;
        end if;
        update store_credits
            set current_balance = current_balance - adjustment,
            available_balance = available_balance + refund
            where id = new.store_credit_id;
    elsif new.state = 'cancellationCapture'  then
        update store_credits
            set current_balance = current_balance + adjustment,
            available_balance = available_balance + adjustment
            where id = new.store_credit_id;
    else
        raise exception 'Failed to update store credit balance with id %, the state % is unknown', new.store_credit_id, new.state;
    end if;
    return new;
end;
$$ language plpgsql;
