create table order_notes (
    id serial primary key,
    order_id integer not null,
    store_admin_id integer not null,
    note_text character varying(255) not null,
    foreign key (order_id) references orders(id) on update restrict on delete cascade,
    foreign key (store_admin_id) references store_admins(id) on update restrict on delete restrict
);

