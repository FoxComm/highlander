create materialized view coupon_codes_search_view as
select
    c.id,
    c.code,
    cp.form_id as coupon_id,
    cp.promotion_id,
    0 as total_used, --this needs to be computed
    c.created_at as created_at
from 
    coupon_codes as c,
	coupons as cp
where 
    cp.form_id = c.coupon_form_id;

create unique index coupon_codes_search_view_idx on coupon_codes_search_view (id);
