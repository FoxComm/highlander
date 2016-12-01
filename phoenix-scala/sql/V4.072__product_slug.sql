alter table products add column slug  generic_string;

create unique index product_slug_idx on products(lower(slug), context_id);

alter table products_catalog_view add column slug generic_string;

alter table products_search_view add column slug generic_string;
