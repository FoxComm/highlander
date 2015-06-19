create table inventory_classifications (
    id serial primary key,
    sku_id integer not null,
    can_sell boolean not null,
    can_pre_order boolean not null,
    can_back_order boolean not null
    foreign key (sku_id) references skus(id) on update restrict on delete restrict
);

