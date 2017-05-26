create table catalogs (
  id bigint primary key,
  scope exts.ltree not null,
  name generic_string not null,
  country_id integer not null references countries(id) on update restrict on delete restrict,
  default_language generic_string not null,
  created_at generic_timestamp,
  updated_at generic_timestamp
);

create index catalogs_and_scope_idx on catalogs (scope);