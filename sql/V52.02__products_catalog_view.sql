create materialized view products_catalog_view as
select
    p.id,
    f.id as product_id,
    context.name as context,
    f.attributes->>(s.attributes->'title'->>'ref') as title,
    f.attributes->(s.attributes->'images'->>'ref') as images,
    f.attributes->>(s.attributes->'description'->>'ref') as description
from
	products as p,
	object_forms as f,
	object_shadows as s,
	object_contexts as context
where
	p.context_id = context.id and
	f.id = p.form_id and
	s.id = p.shadow_id and
    (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < CURRENT_TIMESTAMP and
    (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS NOT FALSE or
    ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= CURRENT_TIMESTAMP));

create unique index products_catalog_view_idx on products_catalog_view (id, context);
