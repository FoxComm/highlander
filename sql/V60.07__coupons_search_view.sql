create materialized view coupons_search_view as
select
    f.id,
    cp.promotion_id,
    context.name as context,
    f.attributes->>(s.attributes->'name'->>'ref') as name,
    f.attributes->>(s.attributes->'storefrontName'->>'ref') as storefront_name,
    f.attributes->>(s.attributes->'description'->>'ref') as description,
    f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
    f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
    0 as total_used, --this needs to be computed
    f.created_at as created_at
from 
	object_contexts as context,
	coupons as cp, 
	object_forms as f, 
	object_shadows as s
where 
	cp.context_id = context.id and
	f.id = cp.form_id and 
	s.id = cp.shadow_id;

create unique index coupons_search_view_code_idx on coupons_search_view (id);
