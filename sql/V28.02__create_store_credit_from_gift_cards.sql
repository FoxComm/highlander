create table store_credit_from_gift_cards (
    id integer primary key,
    gift_card_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references store_credit_origins(id) on update restrict on delete restrict,
    foreign key (gift_card_id) references gift_cards(id) on update restrict on delete restrict
);

create trigger set_store_credits_store_credit_from_gift_cards_id
    before insert
    on store_credit_from_gift_cards
    for each row
    execute procedure set_store_credit_origin_id();

