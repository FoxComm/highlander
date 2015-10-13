create domain note_body text check (length(value) > 0 and length(value) <= 1000);

create table notes (
    id serial primary key,
    store_admin_id integer not null,
    reference_id integer not null,
    reference_type generic_string not null,
    body note_body not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    deleted_by int references store_admins(id) on update restrict on delete restrict,
    foreign key (store_admin_id) references store_admins(id) on update restrict on delete restrict
);