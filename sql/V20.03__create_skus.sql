create table skus (
    id serial primary key,
    product_id integer not null references products(id) on update restrict on delete restrict,
    code generic_string,
    type generic_string,
    attributes jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc'),

    foreign key (product_id) references products(id) on update restrict on delete restrict
);

create unique index sku_idx on skus (id);
create index sku_codex on skus (code);
