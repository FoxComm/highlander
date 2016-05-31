create table coupon_code_usages(
    id serial primary key,
    coupon_form_id integer not null references object_forms(id) on update restrict on delete restrict,
    coupon_code_id integer not null references coupon_codes(id) on update restrict on delete restrict,
    count integer not null default 0,
    updated_at generic_timestamp,
    created_at generic_timestamp
);

create index coupon_code_usages_coupon_form_idx on coupon_code_usages (coupon_form_id);
create index coupon_code_usages_coupon_code_idx on coupon_code_usages (coupon_code_id);