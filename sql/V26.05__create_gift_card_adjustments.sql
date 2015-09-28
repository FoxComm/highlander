-- ledger for all adjustments (credits/debits) to gift_cards
create table gift_card_adjustments (
    id serial primary key,
    gift_card_id integer not null,
    order_payment_id integer not null,
    credit integer not null default 0,
    debit integer not null default 0,
    available_balance integer not null default 0,
    status character varying(255) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (gift_card_id) references gift_cards(id) on update restrict on delete restrict,
    foreign key (order_payment_id) references order_payments(id) on update restrict on delete restrict,
    -- both credit/debit are unsigned (never negative) and only one can be > 0
    constraint valid_entry check ((credit >= 0 and debit >= 0) and (credit > 0 or debit > 0) and
        not (credit > 0 and debit > 0)),
    constraint valid_status check (status in ('auth','canceled','capture'))
);

create function update_gift_card_current_balance() returns trigger as $$
declare
    adjustment integer default 0;
begin
    if new.debit > 0 then
        adjustment := -new.debit;
    elsif new.credit > 0 then
        adjustment := new.credit;
    end if;

    -- canceling an adjustment should remove its monetary change from the gc
    if new.status = 'canceled' and old.status != 'canceled' then
        if old.status = 'capture' then
            update gift_cards
                set current_balance = current_balance - adjustment,
                    available_balance = available_balance - adjustment
                    where id = new.gift_card_id;
            return new;
        else
            update gift_cards set available_balance = available_balance - adjustment where id = new.gift_card_id;
            return new;
        end if;
    end if;

    -- handle credit or debit for auth or capture
    if new.status = 'capture' then
        update gift_cards
            set current_balance = current_balance + adjustment,
                available_balance = available_balance + adjustment
                where id = new.gift_card_id;
    else
        update gift_cards set available_balance = available_balance + adjustment where id = new.gift_card_id;
    end if;

    return new;
end;
$$ language plpgsql;

create trigger set_gift_card_current_balance_trg
    after insert
    on gift_card_adjustments
    for each row
    execute procedure update_gift_card_current_balance();

create trigger update_gift_card_current_balance_trg
    after update
    on gift_card_adjustments
    for each row
    execute procedure update_gift_card_current_balance();

create index gift_card_adjustments_payment_status_idx on gift_card_adjustments (order_payment_id, status);

