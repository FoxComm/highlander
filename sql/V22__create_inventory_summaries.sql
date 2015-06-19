create table inventory_summaries (
    id serial primary key,
    sku_id integer not null,
    available_on_hand integer not null,
    available_pre_order integer not null,
    available_back_order integer not null,
    outstanding_pre_orders integer not null,
    outstanding_back_orders integer not null,
    foreign key (sku_id) references skus(id) on update restrict on delete restrict
);

