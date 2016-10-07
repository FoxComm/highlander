create table object_links(
  id serial primary key,
  left_id  integer not null references object_shadows(id) on update restrict on delete restrict,
  right_id integer not null references object_shadows(id) on update restrict on delete restrict,
  link_type object_link_type not null,
  created_at generic_timestamp,
  updated_at generic_timestamp,

  foreign key (left_id) references object_shadows(id) on update restrict on delete restrict,
  foreign key (right_id) references object_shadows(id) on update restrict on delete restrict
);

create index object_link_left_idx on object_links (left_id);
create index object_link_right_idx on object_links (right_id);
create index object_link_link_type_idx on object_links (link_type);
create index object_link_left_link_type_idx on object_links (left_id, link_type);
