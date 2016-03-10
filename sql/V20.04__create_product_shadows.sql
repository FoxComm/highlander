create table product_shadows(
    id serial primary key,
    product_id integer not null references products(id) on update restrict on delete restrict,
    attributes jsonb,
    variants generic_string,
    active_from timestamp without time zone null,
    active_to timestamp without time zone null,
    created_at timestamp without time zone default (now() at time zone 'utc'),

    foreign key (product_id) references products(id) on update restrict on delete restrict,
);

create unique index product_shadows_idx on product_shadows (id);
create index product_shadows_product_idx on product_shadows (product_id);
