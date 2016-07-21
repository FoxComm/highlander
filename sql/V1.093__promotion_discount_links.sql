create materialized view promotion_discount_links_view as
select 
	p.form_id as promotion_id, 
    context.id as context_id,
    -- discounts
    case when count(discount) = 0
    then
        '[]'
    else	
		json_agg(discount.form_id)
	end as discounts
from promotions as p,
	object_contexts as context,
    object_links as link,
    discounts as discount
where 
    p.context_id = context.id and
    link.left_id = p.shadow_id and
    discount.shadow_id = link.right_id
group by p.form_id, context.id;
 
create unique index promotion_discount_links_view_idex on promotion_discount_links_view (promotion_id);
