create table catalogs (
  id serial primary key,
  scope exts.ltree not null,
  name generic_string not null,
  site generic_string,
  country_id integer not null references countries(id) on update restrict on delete restrict,
  default_language generic_string not null,
  created_at generic_timestamp not null,
  updated_at generic_timestamp not null
);

create index catalogs_and_scope_idx on catalogs (scope);
