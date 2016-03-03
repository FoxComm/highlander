
create table products(
    id serial primary key,
    attributes jsonb,
    variants jsonb,
    is_active bool not null default true,
    created_at timestamp without time zone default (now() at time zone 'utc')
);
