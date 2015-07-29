create table reasons (
    id serial primary key,
    store_admin_id integer not null,
    body character varying(255) not null,
    parent_id integer,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone,
    foreign key (store_admin_id) references store_admins(id) on update restrict on delete restrict
);

create index reasons_idx on reasons (parent_id);
