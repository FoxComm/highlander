
alter table taxonomies_search_view add column created_at json_timestamp;
alter table taxonomies_search_view add column updated_at json_timestamp;

alter table taxons_search_view add column created_at json_timestamp;
alter table taxons_search_view add column updated_at json_timestamp;
