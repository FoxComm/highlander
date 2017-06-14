create or replace function update_products_search_view_taxonomies_fn() returns trigger as $$
declare product_ids bigint [];
begin
  if tg_op = 'DELETE'
    then
      product_ids := array_agg(old.left_id);
  else
    case tg_table_name
      when 'product_taxon_links'
      then
        product_ids := array_agg(new.left_id);
      when 'taxonomy_taxon_links'
      then
        product_ids := (select array_agg(left_id)
                        from product_taxon_links
                        where product_taxon_links.right_id = new.taxon_id);
    end case;
  end if;

  update products_search_view
  set
    taxonomies = productTaxonomies.taxonomies
  from (select
          psv.id                                as product_id,
          coalesce(t.taxonomies, '[]' :: jsonb) as taxonomies
        from products_search_view as psv
          left join
          (select
             product_id,
             case when count(taxons) = 0
               then
                 '[]' :: jsonb
             else
               jsonb_agg(taxons)
             end as taxonomies
           from
             (select
                product_taxon_links.left_id as product_id,
                full_path [1]               as taxonomy,
                jsonb_build_object('taxonomy', full_path [1],
                                   'taxons', json_agg(full_path [2 :array_length(full_path, 1)] :: text[]))
                  as taxons
              from
                taxonomy_taxon_links
                inner join product_taxon_links on taxonomy_taxon_links.taxon_id = product_taxon_links.right_id
              group by product_taxon_links.left_id, taxonomy) as y
           group by y.product_id) as t on t.product_id = psv.id) as productTaxonomies
  where productTaxonomies.product_id = products_search_view.id and products_search_view.id = any (product_ids);

  return null;
end;
$$ language plpgsql;

create or replace function update_products_search_view_catalogs_fn() returns trigger as $$
begin
  update products_search_view
  set
    catalogs = catalogProducts.catalog_names
  from (select
          case when count(cp.id) = 0
            then
              '[]' :: jsonb
            else
              jsonb_agg(c.name)
          end as catalog_names
        from catalogs as c
          left join catalog_products as cp on (cp.catalog_id = c.id and cp.archived_at is null)
        where cp.product_id = new.product_id) as catalogProducts
  where products_search_view.product_id = new.product_id;

  return null;
end;
$$ language plpgsql;

drop trigger if exists update_products_search_view_taxonomies on product_taxon_links;
create trigger update_products_search_view_taxonomies
after insert or update or delete on product_taxon_links
  for each row
  execute procedure update_products_search_view_taxonomies_fn();

drop trigger if exists update_products_search_view_taxonomies_from_taxonomy
on taxonomy_taxon_links;
create trigger update_products_search_view_taxonomies_from_taxonomy
after update on taxonomy_taxon_links
for each row
when (old.full_path is distinct from new.full_path)
execute procedure update_products_search_view_taxonomies_fn();

drop trigger if exists update_products_search_view_catalogs on catalog_products;
create trigger update_products_search_view_catalogs
after insert or update on catalog_products
  for each row
  execute procedure update_products_search_view_catalogs_fn();

