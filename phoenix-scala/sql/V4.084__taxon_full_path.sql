alter table taxonomy_taxon_links
  add column full_path text [];

create or replace function calculate_taxon_full_path(integer)
  returns text [] as $$
begin
  return (
    select coalesce(parent_link.full_path, '{}' :: text [] || illuminate_text(tmf, tms, 'name'), '{}' :: text []) ||
           coalesce(illuminate_text(tf, ts, 'name'), '')
    from taxonomy_taxon_links as link
      inner join taxonomies as tm on link.taxonomy_id = tm.id
      inner join object_forms as tmf on tmf.id = tm.form_id
      inner join object_shadows as tms on tms.id = tm.shadow_id

      inner join taxons as t on link.taxon_id = t.id
      inner join object_forms as tf on tf.id = t.form_id
      inner join object_shadows as ts on ts.id = t.shadow_id

      left join taxonomy_taxon_links as parent_link
        on parent_link.taxonomy_id = link.taxonomy_id

           and link.path != ''
           and cast(parent_link.index as varchar) =
               exts.ltree2text(exts.subpath(NULLIF(link.path, '' :: exts.ltree), -1, 1))
    where link.id = $1);
end; $$ language plpgsql;

create or replace function calculate_taxon_full_path_on_link_insert_fn()
  returns trigger as $$
begin
  update taxonomy_taxon_links
  set full_path = calculate_taxon_full_path(new.id)
  where
    taxonomy_taxon_links.id = new.id;
  return null;
end;
$$ language plpgsql;

create or replace function update_full_path_for_single_link_fn()
  returns trigger as $$
begin
  case tg_table_name
    when 'taxonomies'
    then
      update taxonomy_taxon_links
      set full_path = calculate_taxon_full_path(taxonomy_taxon_links.id)
      where taxonomy_taxon_links.taxonomy_id = new.id and exts.nlevel(taxonomy_taxon_links.path) = 0;
    when 'taxons'
    then
      update taxonomy_taxon_links
      set full_path = calculate_taxon_full_path(taxonomy_taxon_links.id)
      where taxonomy_taxon_links.taxon_id = new.id;
    when 'taxonomy_taxon_links'
    then
      update taxonomy_taxon_links
      set full_path = calculate_taxon_full_path(taxonomy_taxon_links.id)
      where taxonomy_taxon_links.id = new.id;
  end case;

  return null;
end;
$$ language plpgsql;

create or replace function update_full_path_for_children_fn()
  returns trigger as $$
begin
  update taxonomy_taxon_links
  set full_path = new.full_path || full_path[array_length(full_path, 1)]
  where taxonomy_id = new.taxonomy_id and path = new.path || (new.index :: varchar);

  return null;
end;
$$ language plpgsql;

drop trigger if exists calculate_full_path_on_taxonomy_update
on taxonomies;
create trigger calculate_full_path_on_taxonomy_update
after update on taxonomies
for each row
execute procedure update_full_path_for_single_link_fn();

drop trigger if exists calculate_full_path_on_taxon_update
on taxons;
create trigger calculate_full_path_on_taxon_update
after update on taxons
for each row
execute procedure update_full_path_for_single_link_fn();

drop trigger if exists calculate_full_path_on_link_update
on taxonomy_taxon_links;
create trigger calculate_full_path_on_link_update
after update on taxonomy_taxon_links
for each row
when (old.path operator (exts.<>) new.path)
execute procedure update_full_path_for_single_link_fn();

drop trigger if exists populate_full_path_for_children
on taxonomy_taxon_links;
create trigger populate_full_path_for_children
after update on taxonomy_taxon_links
for each row
when (old.full_path is distinct from new.full_path)
execute procedure update_full_path_for_children_fn();

drop trigger if exists calculate_full_path_on_link_insert
on taxonomy_taxon_links;
create trigger calculate_full_path_on_link_insert
after insert on taxonomy_taxon_links
for each row
execute procedure calculate_taxon_full_path_on_link_insert_fn();

