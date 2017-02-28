create table activity_handlers(
  id serial primary key,
  scope exts.ltree not null,
  context_id integer not null references object_contexts(id) on update restrict on delete restrict,
  shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
  form_id integer not null references object_forms(id) on update restrict on delete restrict,
  commit_id integer references object_commits(id) on update restrict on delete restrict,
  updated_at generic_timestamp,
  created_at generic_timestamp,
  archived_at generic_timestamp
);

create index activity_handlers_object_context_idx on activity_handlers (context_id);
create index activity_handler_form_idx on activity_handlers (form_id);
