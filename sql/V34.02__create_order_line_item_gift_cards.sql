create table order_line_item_gift_cards (
    id integer primary key,
    order_id integer not null references orders(id)  on update restrict on delete restrict,
    gift_card_id integer not null references gift_cards(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references order_line_item_origins(id) on update restrict on delete restrict
);

create index order_line_item_gift_cards_gift_card_idx on order_line_item_gift_cards (gift_card_id);

create trigger set_order_line_item_gift_card_id
    before insert
    on order_line_item_gift_cards
    for each row
    execute procedure set_order_line_item_origin_id();

