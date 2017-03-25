alter table products_catalog_view add column taxonomies jsonb default '[]'::jsonb;

update products_catalog_view set taxonomies = psv.taxonomies 
   from products_search_view psv where psv.id = products_catalog_view.id;