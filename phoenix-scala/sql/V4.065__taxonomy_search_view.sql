create table taxonomies_search_view (
  id           integer primary key,
  taxonomy_id  integer,
  name         generic_string,
  context      generic_string,
  type         generic_string,
  values_count int,
  active_from  json_timestamp,
  active_to    json_timestamp,
  archived_at  json_timestamp
);
create unique index taxonomies_search_view_idx
  on taxonomies_search_view (id, lower(context));
