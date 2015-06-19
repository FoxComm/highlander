create table customers (
    id serial primary key,
    email character varying(255) not null,
    hashed_password character varying(255) not null,
    first_name character varying(255),
    last_name character varying(255),
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);
