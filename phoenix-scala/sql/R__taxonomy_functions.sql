
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
           and parent_link.archived_at is null
    where link.id = $1);
end; $$ language plpgsql;
