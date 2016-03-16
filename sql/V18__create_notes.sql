create table notes (
    id serial primary key,
    store_admin_id integer not null references store_admins(id) on update restrict on delete restrict,
    reference_id integer not null,
    reference_type note_reference_type not null,
    body note_body not null,
    priority generic_string not null default 'default',
    created_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    deleted_by int references store_admins(id) on update restrict on delete restrict
);