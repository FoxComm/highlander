create table gift_card_refunds (
    id integer primary key,
    rma_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references gift_card_origins(id) on update restrict on delete restrict,
    foreign key (rma_id) references rmas(id) on update restrict on delete restrict
);

create index gift_card_refunds_rma_idx on gift_card_refunds (rma_id);

create trigger set_gift_card_refunds_id
    before insert
    on gift_card_refunds
    for each row
    execute procedure set_gift_card_origin_id();

