create table product_contexts(
    id serial primary key,
    context jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc')
);
