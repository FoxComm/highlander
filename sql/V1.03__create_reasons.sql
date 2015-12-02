create table reasons (
    id serial primary key,
    reason_type reason_type not null,
    store_admin_id integer not null,
    body generic_string not null,
    parent_id integer,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone,
    foreign key (store_admin_id) references store_admins(id) on update restrict on delete restrict
);

create index reasons_idx on reasons (parent_id);
