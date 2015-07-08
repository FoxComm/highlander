create table gift_card_from_store_credits (
    id integer primary key,
    store_credit_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references gift_card_origins(id) on update restrict on delete restrict,
    foreign key (store_credit_id) references store_credits(id) on update restrict on delete restrict
);

create trigger set_gift_card_from_store_credits_id
    before insert
    on gift_card_from_store_credits
    for each row
    execute procedure set_gift_card_origin_id();

