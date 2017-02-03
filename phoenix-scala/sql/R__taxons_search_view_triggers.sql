create or replace function insert_taxons_search_view_from_taxons_fn()
  returns trigger as $$
begin
  insert into taxons_search_view
    select
      t.id                                                                as id,
      null                                                                as taxonomy_id,
      t.form_id                                                           as taxon_id,
      null                                                                as parent_id,
      t.scope                                                             as scope,
      illuminate_text(f, s, 'name')                                       as name,
      context.name                                                        as context,
      illuminate_text(f, s, 'activeFrom')                                 as active_from,
      illuminate_text(f, s, 'activeTo')                                   as active_to,
      to_char(t.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')             as archived_at
    from taxons as t
      inner join object_contexts as context on (t.context_id = context.id)
      inner join object_forms as f on (f.id = t.form_id)
      inner join object_shadows as s on (s.id = t.shadow_id)
    where t.id = new.id;
  return null;
end;
$$ language plpgsql;


create or replace function update_taxons_search_view_from_taxons_fn()
  returns trigger as $$
declare taxon_ids integer [];
begin
  case tg_table_name
    when 'taxons'
    then
      taxon_ids := array_agg(new.id);
    when 'taxonomy_taxon_links'
    then
      select array_agg(taxon_id)
      from taxonomy_taxon_links
      where id = new.id and archived_at is null
      into taxon_ids;
  end case;


  update taxons_search_view
  set
    taxon_id = t.form_id,
    taxonomy_id  = tm.form_id,
    scope        = t.scope,
    parent_id    = parent.form_id,
    name         = illuminate_text(f, s, 'name'),
    context      = context.name,
    active_from  = illuminate_text(f, s, 'activeFrom'),
    active_to    = illuminate_text(f, s, 'activeTo'),
    archived_at  = to_char(t.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
  from taxons as t
    inner join object_contexts as context on (t.context_id = context.id)
    inner join object_forms as f on (f.id = t.form_id)
    inner join object_shadows as s on (s.id = t.shadow_id)
    left join taxonomy_taxon_links as link on link.taxon_id = t.id and link.archived_at is null
    left join taxonomies as tm on link.taxonomy_id = tm.id
    left join taxonomy_taxon_links as parent_link
      on parent_link.taxonomy_id = link.taxonomy_id
         and cast (link.path as varchar) != ''
         and cast(parent_link.index as varchar) =
             exts.ltree2text(exts.subpath(NULLIF(link.path, '' :: exts.ltree), -1, 1))
    left join taxons as parent on parent_link.taxon_id = parent.id
  where t.id = any (taxon_ids) and t.id = taxons_search_view.id;

  return null;
end;
$$ language plpgsql;

drop trigger if exists insert_taxons_search_view_from_taxons on taxons;
create trigger insert_taxons_search_view_from_taxons
after insert on taxons
for each row
execute procedure insert_taxons_search_view_from_taxons_fn();


drop trigger if exists update_taxons_search_view_from_taxons_fn on taxons;
create trigger update_taxons_search_view_from_taxons_fn
after update on taxons
for each row
execute procedure update_taxons_search_view_from_taxons_fn();


drop trigger if exists update_taxons_search_view_from_taxonomy_taxon_links_fn on taxonomy_taxon_links;
create trigger update_taxons_search_view_from_taxonomy_taxon_links_fn
after insert or update on taxonomy_taxon_links
for each row
execute procedure update_taxons_search_view_from_taxons_fn();
