create table generic_objects(
  id serial primary key,
  scope exts.ltree not null,
  kind generic_string not null,
  context_id integer not null references object_contexts(id) on update restrict on delete restrict,
  shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
  form_id integer not null references object_forms(id) on update restrict on delete restrict,
  commit_id integer references object_commits(id) on update restrict on delete restrict,
  updated_at generic_timestamp,
  created_at generic_timestamp,
  archived_at generic_timestamp
);

create index generic_objects_object_context_idx on generic_objects (context_id);
create index generic_object_form_idx on generic_objects (form_id);

--maps activity handlers to activities of a certain kind
create table activity_kind_handlers(
  id serial primary key,
  scope exts.ltree not null,
  kind generic_string,
  generic_object integer not null references generic_objects(id) on update restrict on delete restrict,
  created_at generic_timestamp
);

--maps activity handlers to activities of a certain kind and objectId/dimension
create table activity_kind_object_handlers(
  id serial primary key,
  scope exts.ltree not null,
  kind generic_string,
  dimension generic_string,
  object_id generic_string
  generic_object_head integer not null references generic_objects(id) on update restrict on delete restrict,
  created_at generic_timestamp
);

create index activity_kind_handler_kindidx on activity_kind_handlers(scope, kind);
create index activity_kind_object_handler_kind_dim_obj_idx on activity_kind_object_handlers(scope, kind, dimension, object_id);
