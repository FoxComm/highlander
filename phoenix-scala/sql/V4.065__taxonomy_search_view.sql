create table taxonomies_search_view (
  id           integer not null unique,
  taxonomy_id  integer,
  name         generic_string,
  context      generic_string,
  type         generic_string,
  values_count int,
  active_from  generic_string,
  active_to    generic_string,
  archived_at  generic_string
);
create unique index taxonomies_search_view_idx
  on taxonomies_search_view (id, lower(context));
