create table variants(
  id serial primary key,
  context_id integer not null references object_contexts(id) on update restrict on delete restrict,
  shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
  form_id integer not null references object_forms(id) on update restrict on delete restrict,
  commit_id integer references object_commits(id) on update restrict on delete restrict,
  updated_at generic_timestamp,
  created_at generic_timestamp
);

create index variants_object_context_idx on variants (context_id);
create index variants_variant_form_idx on variants (context_id);
