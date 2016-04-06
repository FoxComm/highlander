create table coupons(
    id serial primary key,
    promotion_id integer not null references object_forms(id) on update restrict on delete restrict,
    context_id integer not null references object_contexts(id) on update restrict on delete restrict,
    shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    form_id integer not null references object_forms(id) on update restrict on delete restrict,
    commit_id integer references object_commits(id) on update restrict on delete restrict,
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index coupons_object_context_idx on coupons (context_id);
create index coupons_coupon_form_idx on coupons (form_id);
create index coupons_promotion_idx on coupons (promotion_id);

