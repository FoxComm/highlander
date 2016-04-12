create materialized view products_search_view as
select
    p.id as id,
    f.id as product_id, 
    context.name as context,
    f.attributes->>(s.attributes->'title'->>'ref') as title,
    f.attributes->(s.attributes->'images'->>'ref') as images,
    f.attributes->>(s.attributes->'description'->>'ref') as description,
    f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
    f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
    link.skus as skus
from 
	products as p, 
	object_forms as f, 
	object_shadows as s, 
	object_contexts as context,
	product_sku_links_view as link
where 
	p.context_id = context.id and
	f.id = p.form_id and 
	s.id = p.shadow_id and 
	link.product_id = p.id;

create unique index products_search_view_idx on products_search_view (id, context);
