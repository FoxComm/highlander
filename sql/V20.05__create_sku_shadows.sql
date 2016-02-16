create table sku_shadows(
    id serial primary key,
    product_context_id integer,
    sku_id integer,
    attributes jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (product_context_id) references product_contexts(id) on update restrict on delete restrict
);
