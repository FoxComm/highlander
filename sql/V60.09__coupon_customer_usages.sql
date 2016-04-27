create table coupon_customer_usages(
    id serial primary key,
    coupon_form_id integer not null references object_forms(id) on update restrict on delete restrict,
    customer_id integer not null references customers(id) on update restrict on delete restrict,
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index coupon_customer_usages_coupon_form_idx on coupon_customer_usages (coupon_form_id);

