create table content_types (
  id bigserial,
  name varchar,
  schema jsonb,
  inserted_at timestamptz not null,
  updated_at timestamptz not null,
  sys_period tstzrange not null default tstzrange(current_timestamp, null)
);

create table content_types_history (like content_types);

create trigger versioning_trigger
before insert or update or delete on content_types
  for each row execute procedure versioning(
    'sys_period', 'content_types_history', true
  );