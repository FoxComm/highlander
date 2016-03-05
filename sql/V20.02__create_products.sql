
create table products(
    id serial primary key,
    attributes jsonb,
    variants jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create unique index product_idx on products (id);
