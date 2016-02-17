-- ledger for all adjustments (credits/debits) to gift_cards
create table gift_card_adjustments (
    id serial primary key,
    gift_card_id integer not null,
    order_payment_id integer null,
    store_admin_id integer null,
    credit integer not null default 0,
    debit integer not null default 0,
    available_balance integer not null default 0,
    state character varying(255) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (gift_card_id) references gift_cards(id) on update restrict on delete restrict,
    foreign key (order_payment_id) references order_payments(id) on update restrict on delete restrict,
    -- both credit/debit are unsigned (never negative) and only one can be > 0
    constraint valid_entry check (
        (
            (credit >= 0 and debit >= 0) and 
            (credit > 0 or debit > 0) and 
            not (credit > 0 and debit > 0) and 
            state <> 'cancellationCapture'
        ) or 
        (credit >= 0 and debit >= 0 and state = 'cancellationCapture')
    ),
    constraint valid_state check (state in ('auth','canceled','capture','cancellationCapture'))
);

create function update_gift_card_current_balance() returns trigger as $$
declare
    adjustment integer default 0;
    new_available_balance integer default 0;
begin
    if new.debit > 0 then
        adjustment := -new.debit;
    elsif new.credit > 0 then
        adjustment := new.credit;
    end if;

    -- canceling an adjustment should remove its monetary change from the gc
    if new.state = 'canceled' and old.state != 'canceled' then
        if old.state = 'capture' or old.state = 'cancellationCapture' then
            update gift_cards
                set current_balance = current_balance - adjustment,
                    available_balance = available_balance - adjustment
                where id = new.gift_card_id
                returning available_balance into new_available_balance;
        else
            update gift_cards
                set available_balance = available_balance - adjustment
                where id = new.gift_card_id
                returning available_balance into new_available_balance;
        end if;

        new.available_balance = new_available_balance;
        return new;
    end if;

    -- handle credit or debit for auth or capture
    if new.state = 'capture' then
        update gift_cards
            set current_balance = current_balance + adjustment,
                available_balance = available_balance + adjustment
            where id = new.gift_card_id
            returning available_balance into new_available_balance;
    else
        update gift_cards
            set available_balance = available_balance + adjustment
            where id = new.gift_card_id
            returning available_balance into new_available_balance;
    end if;

    new.available_balance = new_available_balance;
    return new;
end;
$$ language plpgsql;

create trigger set_gift_card_current_balance_trg
    before insert
    on gift_card_adjustments
    for each row
    execute procedure update_gift_card_current_balance();

create trigger update_gift_card_current_balance_trg
    before update
    on gift_card_adjustments
    for each row
    execute procedure update_gift_card_current_balance();

create index gift_card_adjustments_payment_state_idx on gift_card_adjustments (order_payment_id, state);
create index gift_card_adjustments_gift_card_idx on gift_card_adjustments (gift_card_id);
