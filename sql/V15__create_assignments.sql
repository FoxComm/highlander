create table assignments (
    id serial primary key,
    assignment_type assignment_type not null,
    store_admin_id integer not null references store_admins(id) on update restrict on delete restrict,
    reference_id integer not null,
    reference_type assignment_ref_type not null,
    created_at timestamp without time zone default (now() at time zone 'utc')
);