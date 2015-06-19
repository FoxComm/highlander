create table shipments (
    id bigint primary key,
    order_id bigint not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    foreign key (order_id) references orders(id) on update restrict on delete restrict
);

