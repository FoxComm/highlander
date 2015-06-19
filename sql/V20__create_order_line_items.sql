create table order_line_items (
    id serial primary key,
    order_id integer not null,
    sku_id integer not null,
    status character varying(255),
    created_at timestamp without time zone default (now() at time zone 'utc')
);

alter table only order_line_items
    add constraint order_line_items_order_id_fk foreign key (order_id) references orders(id) on update restrict on delete restrict;