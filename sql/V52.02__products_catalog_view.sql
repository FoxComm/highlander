create table products_catalog_view
(
    id integer,
    product_id integer,
    context generic_string,
    title text,
    images jsonb,
    description text,
    sale_price text,
    currency text,
    tags text
);
create unique index products_catalog_view_idx on products_catalog_view (id, lower(context));
