create table order_line_items (
    id serial primary key,
    order_id integer not null references orders(id) on update restrict on delete restrict,
    origin_id integer not null references order_line_item_origins(id) on update restrict on delete restrict,
    origin_type character varying(255) not null,
    status character varying(255) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    constraint valid_origin_type check (origin_type in ('skuItem', 'giftCardItem')),
    constraint valid_status check (status in ('cart', 'pending', 'preOrdered', 'backOrdered', 'canceled', 'shipped'))
);

create index order_line_items_order_and_origin_idx on order_line_items (order_id, origin_id)