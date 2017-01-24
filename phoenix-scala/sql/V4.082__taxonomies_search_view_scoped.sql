alter table taxonomies_search_view
  add column scope exts.ltree;

update taxonomies_search_view
set scope = taxonomies.scope from taxonomies
where taxonomies.id = taxonomies_search_view.id;

alter table taxonomies_search_view alter column scope set not null;
