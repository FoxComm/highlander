create materialized view products_catalog_view as
select
    p.id,
    f.id as product_id,
    context.name as context,
    f.attributes->>(s.attributes->'title'->>'ref') as title,
    f.attributes->(s.attributes->'images'->>'ref') as images,
    f.attributes->>(s.attributes->'description'->>'ref') as description,
    sku.price as sale_price
from
	object_contexts as context,
	products as p,
	object_forms as f,
	object_shadows as s,
	product_sku_links_view as sv,
    sku_search_view as sku
    
where
	p.context_id = context.id and
	f.id = p.form_id and
	s.id = p.shadow_id and
    sv.product_id = p.id and --get list of sku codes for the product
    sku.code = sv.skus->>0 and --get first sku code
    sku.context_id = context.id and --make sure we are in same context
    (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < CURRENT_TIMESTAMP and
    (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS NOT FALSE or
    ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= CURRENT_TIMESTAMP));

create unique index products_catalog_view_idx on products_catalog_view (id, context);
