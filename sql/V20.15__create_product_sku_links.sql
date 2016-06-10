create table product_sku_links(
    id serial primary key,
    left_id  integer not null references products(id) on update restrict on delete restrict,
    right_id integer not null references skus(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    updated_at generic_timestamp,

    foreign key (left_id) references products(id) on update restrict on delete restrict,
    foreign key (right_id) references skus(id) on update restrict on delete restrict
);

create index product_sku_link_left_idx on product_sku_links (left_id);
create index product_sku_link_right_idx on product_sku_links (right_id);
