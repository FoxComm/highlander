create table orders (
    id serial primary key,
    customer_id integer,
    status character varying(255) not null,
    locked int,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

