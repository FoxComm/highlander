create table notes (
    id serial primary key,
    store_admin_id integer not null,
    reference_id integer not null,
    reference_type character varying(255) not null,
    text character varying(1000) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (store_admin_id) references store_admins(id) on update restrict on delete restrict
);

