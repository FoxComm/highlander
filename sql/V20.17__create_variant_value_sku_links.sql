create table variant_value_sku_links(
  id serial primary key,
  left_id  integer not null references variant_values(id) on update restrict on delete restrict,
  right_id integer not null references skus(id) on update restrict on delete restrict,
  created_at generic_timestamp,
  updated_at generic_timestamp,

  foreign key (left_id) references variant_values(id) on update restrict on delete restrict,
  foreign key (right_id) references skus(id) on update restrict on delete restrict
);

create index variant_value_sku_link_left_idx on variant_value_sku_links (left_id);
create index variant_value_sku_link_right_idx on variant_value_sku_links (right_id);
