drop trigger if exists refresh_products_cat_search_view_from_p_skus_view on product_to_variant_links_view;
drop trigger if exists refresh_products_cat_search_view_from_skus_view on product_variants_search_view;

-- Should probably change to update only for these
create trigger refresh_products_cat_search_view_from_p_variant_view
  after insert or update on product_to_variant_links_view
  for each row
  execute procedure refresh_products_cat_search_view_fn();

create trigger refresh_products_cat_search_view_from_variant_view
  after insert or update on product_variants_search_view
  for each row
  execute procedure refresh_products_cat_search_view_fn();
