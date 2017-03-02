create trigger refresh_products_cat_search_view_from_skus_view
  after insert or update on product_variants_search_view
  for each row
  execute procedure refresh_products_cat_search_view_fn();
