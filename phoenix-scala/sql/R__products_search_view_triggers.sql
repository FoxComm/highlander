create or replace function update_products_search_view_taxonomies_fn() returns trigger as $$
declare product_ids bigint [];
begin
  case tg_table_name
    when 'product_taxon_links'
    then
      product_ids := array_agg(new.left_id);
    when 'taxonomy_taxon_links'
    then
      product_ids := (select left_id
                      from product_taxon_links
                      where product_taxon_links.right_id = new.taxon_id);
  end case;

  update products_search_view
  set
    taxonomies = productTaxonomies.taxonomies
  from (select
          product_id,
          case when count(taxonomy_taxons) = 0
            then
              '[]' :: jsonb
          else
            jsonb_agg(taxonomy_taxons)
          end as taxonomies
        from
          (select
             product_id,
             json_build_object('taxonomy', taxonomy, 'taxons', taxons) as taxonomy_taxons
           from
             (select
                product_taxon_links.left_id                                    as product_id,
                full_path [1]                                                  as taxonomy,
                json_agg(full_path [2 :array_length(full_path, 1)] :: text []) as taxons

              from
                taxonomy_taxon_links
                inner join product_taxon_links on taxonomy_taxon_links.taxon_id = product_taxon_links.right_id
              group by product_taxon_links.left_id, taxonomy) as x) as y
        group by product_id) as productTaxonomies
  where productTaxonomies.product_id = products_search_view.id and products_search_view.id = any (product_ids);

  return null;
end;
$$ language plpgsql;

drop trigger if exists update_products_search_view_taxonomies on product_taxon_links;
create trigger update_products_search_view_taxonomies
after insert or update on product_taxon_links
  for each row
  execute procedure update_products_search_view_taxonomies_fn();

drop trigger if exists update_products_search_view_taxonomies_from_taxonomy
on taxonomy_taxon_links;
create trigger update_products_search_view_taxonomies_from_taxonomy
after update on taxonomy_taxon_links
for each row
when (old.full_path is distinct from new.full_path)
execute procedure update_products_search_view_taxonomies_fn();
