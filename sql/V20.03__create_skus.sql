create table skus (
    id serial primary key,
    product_id integer,
    sku generic_string,
    type generic_string,
    attributes jsonb,
    is_hazardous bool,
    is_active bool,
    foreign key (product_id) references products(id) on update restrict on delete restrict
);
