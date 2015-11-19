create table rma_line_item_gift_cards (
    id integer primary key,
    rma_id integer not null references rmas(id) on update restrict on delete restrict,
    gift_card_id integer not null references gift_cards(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references rma_line_item_origins(id) on update restrict on delete restrict
);

create index rma_line_item_gift_cards_rma_idx on rma_line_item_gift_cards (rma_id);

create trigger set_rma_line_item_gift_card_id
    before insert
    on rma_line_item_gift_cards
    for each row
    execute procedure set_rma_line_item_origin_id();

