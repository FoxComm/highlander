alter table products_catalog_view add column retail_price text;

update products_catalog_view
    set retail_price = subquery.retail_price from (
    select p.id,
        sku.retail_price
    from products as p
    inner join product_sku_links_view as sv on (sv.product_id = p.id)
        inner join object_contexts as context on (p.context_id = context.id)
    inner join sku_search_view as sku on (sku.context_id = context.id and sku.sku_code = sv.skus->>0)
) as subquery
where subquery.id = products_catalog_view.id;
