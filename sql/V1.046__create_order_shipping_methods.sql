create table order_shipping_methods (
    id serial primary key,
    cord_ref text not null,
    shipping_method_id integer not null,
    price integer not null,
    foreign key (cord_ref) references cords(reference_number) on update restrict on delete restrict,
    foreign key (shipping_method_id) references shipping_methods(id) on update restrict on delete restrict
);

create unique index order_shipping_methods_cord_ref_idx on order_shipping_methods (cord_ref);
