create table skus (
    id serial primary key,
    code generic_string,
    type generic_string,
    attributes jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create unique index sku_idx on skus (id);
create index sku_codex on skus (code);
