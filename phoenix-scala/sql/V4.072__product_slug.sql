alter table products add column slug  generic_string;

create unique index product_slug_idx on products(slug, context_id);


