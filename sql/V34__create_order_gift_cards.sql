create table order_gift_cards (
    id serial primary key,
    order_id integer not null,
    gift_card_id integer not null
);

alter table only order_gift_cards
    add constraint order_gift_cards_order_id_fk foreign key (order_id) references orders(id)
        on update restrict on delete restrict;

alter table only order_gift_cards
    add constraint order_gift_cards_gift_card_id_fk foreign key (gift_card_id) references gift_cards(id)
        on update restrict on delete restrict;

create index order_gift_cards_order_and_gift_card_idx on order_gift_cards (order_id, gift_card_id)

