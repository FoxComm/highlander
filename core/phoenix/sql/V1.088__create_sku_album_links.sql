create table sku_album_links(
  id serial primary key,
  left_id  integer not null references skus(id) on update restrict on delete restrict,
  right_id integer not null references albums(id) on update restrict on delete restrict,
  created_at generic_timestamp,
  updated_at generic_timestamp,

  foreign key (left_id) references skus(id) on update restrict on delete restrict,
  foreign key (right_id) references albums(id) on update restrict on delete restrict
);

create index sku_album_link_left_idx on sku_album_links (left_id);
create index sku_album_link_right_idx on sku_album_links (right_id);
