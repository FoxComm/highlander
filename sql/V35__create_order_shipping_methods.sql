create table order_shipping_methods (
    id serial primary key,
    order_id integer not null,
    admin_display_name character varying(255) not null,
    storefront_display_name character varying(255) not null,
    shipping_carrier_id integer null,
    price integer not null,
    foreign key (order_id) references orders(id) on update restrict on delete restrict
);

create unique index order_shipping_methods_order_id_idx on order_shipping_methods (order_id);
