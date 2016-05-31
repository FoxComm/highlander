create table coupon_customer_usages(
    id serial primary key,
    coupon_form_id integer not null references object_forms(id) on update restrict on delete restrict,
    customer_id integer not null references customers(id) on update restrict on delete restrict,
    count integer not null default 0,
    updated_at generic_timestamp,
    created_at generic_timestamp
);

create index coupon_customer_usages_coupon_form_idx on coupon_customer_usages (coupon_form_id);

