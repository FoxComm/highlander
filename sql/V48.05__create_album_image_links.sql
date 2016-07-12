create table album_image_links(
  id serial primary key,
  left_id  integer not null references albums(id) on update restrict on delete restrict,
  right_id integer not null references images(id) on update restrict on delete restrict,
  position integer not null,
  created_at generic_timestamp,
  updated_at generic_timestamp,

  foreign key (left_id) references albums(id) on update restrict on delete restrict,
  foreign key (right_id) references images(id) on update restrict on delete restrict
);

create index album_image_link_left_idx on album_image_links (left_id);
create index album_image_link_right_idx on album_image_links (right_id);
