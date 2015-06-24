create table orders_shipping_methods(
    order_id integer primary key,
    shipping_method_id integer not null,
    shipping_price integer,
    foreign key (order_id) references orders(id) on update restrict on delete cascade,
    foreign key (shipping_method_id) references shipping_methods(id) on update restrict on delete cascade
);