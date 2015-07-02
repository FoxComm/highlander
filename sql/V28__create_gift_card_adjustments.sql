-- ledger for all adjustments (credits/debits) to gift_cards
create table gift_card_adjustments (
    id serial,
    gift_card_id integer not null,
    credit integer not null default 0,
    debit integer not null default 0,
    capture boolean not null default false,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (gift_card_id) references gift_cards(id) on update restrict on delete restrict,
    -- both credit/debit are unsigned (never negative) and only one can be > 0
    constraint valid_entry check ((credit >= 0 and debit >= 0) and (credit > 0 or debit > 0) and
        not (credit > 0 and debit > 0))
);

create trigger update_gift_card_current_balance
    after insert
    on gift_card_adjustments
    for each row
    execute procedure update_gift_card_current_balance();

