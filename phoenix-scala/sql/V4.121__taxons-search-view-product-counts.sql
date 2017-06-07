alter table taxons_search_view
  add column products_count int;

update taxons_search_view
set products_count = (select count(left_id)
                      from product_taxon_links
                      where right_id = taxons_search_view.id and product_taxon_links.archived_at is not null);

alter table taxons_search_view
  alter column products_count set not null;


