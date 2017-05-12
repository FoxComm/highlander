create table channels (
  id serial primary key,
  scope exts.ltree,
  default_context_id integer not null references object_contexts(id),
  draft_context_id integer not null references object_contexts(id),
  location generic_string not null,
  name generic_string not null,
  created_at generic_timestamp,
  updated_at generic_timestamp,
  archived_at generic_timestamp
);

create index channels_default_context_idx on channels (default_context_id);
create index channels_draft_context_idx on channels (draft_context_id);
