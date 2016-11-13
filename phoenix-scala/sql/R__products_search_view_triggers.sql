create or replace function update_products_search_view_taxonomies_fn() returns trigger as $$
begin

  update products_search_view set
    taxonomies = subquery.taxonomies
  from (select
      case  when count(taxons) = 0
      then
        '[]'::jsonb
      else
        json_agg(taxons.form_id)::jsonb end as taxonomies,
      products_search_view.id as id
        from product_taxon_links
        inner join products_search_view on products_search_view.product_id = product_taxon_links.left_id
        inner join taxons on taxons.id = product_taxon_links.right_id
        where product_taxon_links.left_id = new.left_id
        group by products_search_view.id
       ) as subquery

  where products_search_view.id = subquery.id;

  return null;
end;
$$ language plpgsql;

drop trigger if exists update_products_search_view_taxonomies on product_taxon_links;
create trigger update_products_search_view_taxonomies
after insert or update on product_taxon_links
  for each row
  execute procedure update_products_search_view_taxonomies_fn();
