create or replace function insert_taxonomies_search_view_from_taxonomies_fn()
  returns trigger as $$
begin
  insert into taxonomies_search_view
    select
      t.id                                                                as id,
      t.form_id                                                           as taxonomy_id,
      illuminate_text(f, s, 'name')                                       as name,
      context.name                                                        as context,
      (case t.hierarchical when true then 'hierarchical' else 'flat' end) as type,
      (select count(*) from taxonomy_taxon_links as link
      where link.taxonomy_id = t.id and link.archived_at is null)         as values_count,
      illuminate_text(f, s, 'activeFrom')                                 as active_from,
      illuminate_text(f, s, 'activeTo')                                   as active_to,
      to_char(t.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')             as archived_at,
      t.scope                                                             as scope,
      to_char(t.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')              as created_at,
      to_char(t.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')              as updated_at
    from taxonomies as t
      inner join object_contexts as context on (t.context_id = context.id)
      inner join object_forms as f on (f.id = t.form_id)
      inner join object_shadows as s on (s.id = t.shadow_id)
    where t.id = new.id;
  return null;
end;
$$ language plpgsql;


create or replace function update_taxonomies_search_view_from_taxonomies_fn()
  returns trigger as $$
declare taxonomy_ids integer [];
begin
  case tg_table_name
    when 'taxonomies'
    then
      taxonomy_ids := array_agg(new.id);
    when 'taxons'
    then
      select array_agg(taxonomy_id)
      from taxonomy_taxon_links
      where taxon_id = new.id
      into taxonomy_ids;
    when 'taxonomy_taxon_links'
    then
      select array_agg(taxonomy_id)
      from taxonomy_taxon_links
      where id = new.id
      into taxonomy_ids;
  end case;


  update taxonomies_search_view
  set
    taxonomy_id  = t.form_id,
    scope        = t.scope,
    name         = illuminate_text(f, s, 'name'),
    context      = context.name,
    values_count = (select count(*) from taxonomy_taxon_links as link
    where link.taxonomy_id = t.id and link.archived_at is null),
    active_from  = illuminate_text(f, s, 'activeFrom'),
    active_to    = illuminate_text(f, s, 'activeTo'),
    updated_at   = to_char(t.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
    archived_at  = to_char(t.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
  from taxonomies as t
    inner join object_contexts as context on (t.context_id = context.id)
    inner join object_forms as f on (f.id = t.form_id)
    inner join object_shadows as s on (s.id = t.shadow_id)
  where t.id = any (taxonomy_ids) and t.id = taxonomies_search_view.id;

  return null;
end;
$$ language plpgsql;

drop trigger if exists insert_taxonomies_search_view_from_taxonomies on taxonomies;
create trigger insert_taxonomies_search_view_from_taxonomies
after insert on taxonomies
for each row
execute procedure insert_taxonomies_search_view_from_taxonomies_fn();

drop trigger if exists update_taxonomies_search_view_from_taxonomies_fn on taxonomies;
create trigger update_taxonomies_search_view_from_taxonomies_fn
after update on taxonomies
for each row
execute procedure update_taxonomies_search_view_from_taxonomies_fn();

drop trigger if exists update_taxonomies_search_view_from_taxons_fn on taxons;
create trigger update_taxonomies_search_view_from_taxons_fn
after insert or update on taxons
for each row
execute procedure update_taxonomies_search_view_from_taxonomies_fn();

drop trigger if exists update_taxonomies_search_view_from_taxonomy_taxon_links_fn on taxonomy_taxon_links;
create trigger update_taxonomies_search_view_from_taxonomy_taxon_links_fn
after insert or update on taxonomy_taxon_links
for each row
execute procedure update_taxonomies_search_view_from_taxonomies_fn();
