create table product_heads(
    id serial primary key,
    context_id integer not null references product_contexts(id) on update restrict on delete restrict,
    shadow_id integer not null references product_shadows(id) on update restrict on delete restrict,
    product_id integer not null references products(id) on update restrict on delete restrict,
    commit_id integer references product_commits(id) on update restrict on delete restrict,
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index product_heads_product_idx on product_heads (product_id);
create index product_heads_product_context_idx on product_heads (context_id);

