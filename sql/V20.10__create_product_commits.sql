create table product_commits(
    id serial primary key,
    shadow_id integer not null references product_shadows(id) on update restrict on delete restrict,
    product_id integer not null references products(id) on update restrict on delete restrict,
    reason_id integer references reasons(id) on update restrict on delete restrict,
    previous_id integer references product_commits(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index product_commitss_product_idx on product_commitss (product_id);

