create table catalogs_search_view (
  id bigint primary key,
  scope exts.ltree not null,
  name generic_string not null,
  site generic_string,
  country_id integer not null,
  country_name generic_string not null,
  default_language generic_string not null,
  created_at json_timestamp,
  updated_at json_timestamp
);
