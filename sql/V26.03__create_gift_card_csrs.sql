create table gift_card_csrs (
    id integer primary key,
    admin_id integer not null,
    reason character varying(255) not null,
    sub_reason character varying(255) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references gift_card_origins(id) on update restrict on delete restrict,
    foreign key (admin_id) references store_admins(id) on update restrict on delete restrict
);

create index gift_card_csrs_idx on gift_card_csrs (admin_id);

create trigger set_gift_card_csrs_id
    before insert
    on gift_card_csrs
    for each row
    execute procedure set_gift_card_origin_id();

