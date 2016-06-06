create table return_line_item_gift_cards (
    id integer primary key,
    return_id integer not null references returns(id) on update restrict on delete restrict,
    gift_card_id integer not null references gift_cards(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    foreign key (id) references return_line_item_origins(id) on update restrict on delete restrict
);

create index return_line_item_gift_cards_return_idx on return_line_item_gift_cards (return_id);

create trigger set_return_line_item_gift_card_id
    before insert
    on return_line_item_gift_cards
    for each row
    execute procedure set_return_line_item_origin_id();

