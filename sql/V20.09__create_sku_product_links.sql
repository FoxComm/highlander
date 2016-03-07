create table sku_product_links(
    id serial primary key,
    sku_id integer not null references skus(id) on update restrict on delete restrict,
    product_id integer not null references products(id) on update restrict on delete restrict,

    foreign key (sku_id) references skus(id) on update restrict on delete restrict,
    foreign key (product_id) references products(id) on update restrict on delete restrict
);

create unique index sku_product_sku_idx on sku_product_links (sku_id);
create unique index sku_product_product_idx on sku_product_links (product_id);
