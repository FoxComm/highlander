
alter table taxonomies_search_view add column created_at json_timestamp not null;
alter table taxonomies_search_view add column updated_at json_timestamp not null;
