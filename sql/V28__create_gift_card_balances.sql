-- ledger for all changes (credits/debits) to gift_cards
create table gift_card_balances (
    id bigserial,
    gift_card_id integer not null,
    credit integer not null default 0,
    debit integer not null default 0,
    foreign key (gift_card_id) references gift_cards(id) on update restrict on delete restrict,
    -- both credit/debit are unsigned (never negative) and only one can be > 0
    constraint valid_entry check ((credit >= 0 and debit >= 0) and (credit > 0 or debit > 0) and
        not (credit > 0 and debit > 0))
);

