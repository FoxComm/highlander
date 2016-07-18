create materialized view promotions_search_view as
select
    f.id, 
    context.name as context,
    p.apply_type,
    f.attributes->>(s.attributes->'name'->>'ref') as promotion_name,
    f.attributes->>(s.attributes->'storefrontName'->>'ref') as storefront_name,
    f.attributes->>(s.attributes->'description'->>'ref') as description,
    f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
    f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
    0 as total_used, --this needs to be computed
    0 as current_carts, --this needs to be computed
    to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
    link.discounts as discounts
from 
	promotions as p, 
	object_forms as f, 
	object_shadows as s, 
	object_contexts as context,
	promotion_discount_links_view as link
where 
	p.context_id = context.id and
	f.id = p.form_id and 
	s.id = p.shadow_id and 
	link.promotion_id = p.form_id and
    link.context_id = context.id;

create unique index promotions_search_view_idex on promotions_search_view (id, context);
