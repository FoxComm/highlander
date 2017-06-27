create table entities (
  id bigserial,
  kind varchar,
  content jsonb,
  content_type_id bigint,
  schema_version timestamp,
  inserted_at timestamptz not null,
  updated_at timestamptz not null,
  sys_period tstzrange not null default tstzrange(current_timestamp, null)
);

create table entities_history (like entities);

create trigger versioning_trigger
before insert or update or delete on entities
  for each row execute procedure versioning(
    'sys_period', 'entities_history', true
  );