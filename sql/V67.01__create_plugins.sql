create table plugins (
  id          serial primary key,
  name        generic_string unique not null,
  version     generic_string not null,
  description generic_string not null,
  is_disabled boolean not null default false,
  api_host    generic_string not null,
  api_port    integer not null,
  settings    jsonb not null default '{}'::jsonb,
  schema_settings jsonb not null default '[]'::jsonb,

  created_at generic_timestamp not null,
  updated_at generic_timestamp,
  deleted_at generic_timestamp
);

