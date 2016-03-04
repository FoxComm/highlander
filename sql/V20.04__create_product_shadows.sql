create table product_shadows(
    id serial primary key,
    product_context_id integer not null references product_contexts(id) on update restrict on delete restrict,
    product_id integer not null references products(id) on update restrict on delete restrict,
    attributes jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc'),

    foreign key (product_id) references products(id) on update restrict on delete restrict,
    foreign key (product_context_id) references product_contexts(id) on update restrict on delete restrict
);

create unique index product_shadows_idx on product_shadows (id);
create index product_shadows_product_context_idx on product_shadows (product_context_id);
create unique index product_shadows_product_id_product_context_idx on product_shadows (product_id, product_context_id);
create index product_shadows_product_idx on product_shadows (product_id);
