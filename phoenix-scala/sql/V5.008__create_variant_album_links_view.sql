create table variant_album_links_view
(
    product_variant_id integer unique,
    albums jsonb
);
