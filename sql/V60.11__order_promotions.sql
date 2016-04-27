create table order_promotions (
    id serial primary key,
    order_id integer not null references orders(id) on update restrict on delete restrict,
    promotion_shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    apply_type generic_string not null,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index order_promotions_order_idx on order_promotions (order_id);

