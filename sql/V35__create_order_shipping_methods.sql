create table order_shipping_methods (
    id serial primary key,
    order_id integer not null,
    shipping_method_id integer not null,
    foreign key (order_id) references orders(id) on update restrict on delete restrict,
    foreign key (shipping_method_id) references shipping_methods(id) on update restrict on delete restrict
);

create unique index order_shipping_methods_order_id_idx on order_shipping_methods (order_id);
