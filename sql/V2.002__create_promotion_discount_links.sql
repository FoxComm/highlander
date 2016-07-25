create table promotion_discount_links(
    id serial primary key,
    left_id  integer not null references promotions(id) on update restrict on delete restrict,
    right_id integer not null references discounts(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    updated_at generic_timestamp,

    foreign key (left_id) references promotions(id) on update restrict on delete restrict,
    foreign key (right_id) references discounts(id) on update restrict on delete restrict
);

create index promotion_discount_link_left_idx on promotion_discount_links (left_id);
create index promotion_discount_link_right_idx on promotion_discount_links (right_id);
