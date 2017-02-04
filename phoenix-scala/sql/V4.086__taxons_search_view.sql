create table taxons_search_view(
  id           integer primary key,
  taxonomy_id  integer,
  taxon_id  integer,
  parent_id  integer,
  scope ltree,
  name         generic_string,
  context      generic_string,
  active_from  json_timestamp,
  active_to    json_timestamp,
  archived_at  json_timestamp
);

create unique index taxons_search_view_idx
  on taxons_search_view (id, lower(context));
