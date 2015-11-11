create table gift_card_manuals (
    id integer primary key,
    admin_id integer not null,
    reason_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references gift_card_origins(id) on update restrict on delete restrict,
    foreign key (admin_id) references store_admins(id) on update restrict on delete restrict,
    foreign key (reason_id) references reasons(id) on update restrict on delete restrict
);

create index gift_card_manuals_admin_idx on gift_card_manuals (admin_id);

create trigger set_gift_card_manuals_id
    before insert
    on gift_card_manuals
    for each row
    execute procedure set_gift_card_origin_id();

