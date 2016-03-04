create table product_contexts(
    id serial primary key,
    name generic_string,
    attributes jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create unique index product_contexts_idx on product_contexts (id);
create index product_contexts_namex on product_contexts (name);

