create table variant_variant_value_links(
    id serial primary key,
    left_id  integer not null references variants(id) on update restrict on delete restrict,
    right_id integer not null references variant_values(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    updated_at generic_timestamp,

    foreign key (left_id) references variants(id) on update restrict on delete restrict,
    foreign key (right_id) references variant_values(id) on update restrict on delete restrict
);

create index variant_variant_value_link_left_idx on variant_variant_value_links (left_id);
create index variant_variant_value_link_right_idx on variant_variant_value_links (right_id);
