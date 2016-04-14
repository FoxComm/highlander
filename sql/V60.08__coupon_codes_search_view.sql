create materialized view coupon_codes_search_view as
select
    c.id,
    c.code,
    cp.id as coupon_id,
    cp.promotion_id,
    context.name as context,
    0 as total_used, --this needs to be computed
    f.created_at as created_at
from 
    coupon_codes as c,
	object_contexts as context,
	coupons as cp, 
	object_forms as f, 
	object_shadows as s
where 
	cp.context_id = context.id and
	f.id = cp.form_id and 
	s.id = cp.shadow_id;

create unique index coupon_codes_search_view_idx on coupon_codes_search_view (id);
