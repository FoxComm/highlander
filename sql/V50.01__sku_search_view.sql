create materialized view sku_search_view as
select
    sku.id as sku_id,
    sku.code as code,
    product_context.name as context,
    sku.attributes->'title'->>(sku_shadow.attributes->>'title') as title,
    sku.attributes->'price'->(sku_shadow.attributes->>'price')->>'value' as price,
    sku.is_hazardous
from skus as sku, sku_shadows as sku_shadow, product_contexts as product_context 
where sku_shadow.sku_id = sku.id and sku_shadow.product_context_id = product_context.id;

create unique index sku_search_view_idx on sku_search_view (sku_id, context);
