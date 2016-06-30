create table order_promotions (
    id serial primary key,
    order_ref text not null references orders(reference_number) on update restrict on delete restrict,
    promotion_shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    apply_type generic_string not null,
    coupon_code_id int null,
    created_at generic_timestamp
);

create index order_promotions_order_idx on order_promotions (order_ref);

