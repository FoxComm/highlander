create materialized view product_sku_links_view as
select 
	p.id as product_id, 
    -- SKUs
    case when count(sku) = 0
    then
        '[]'
    else	
		json_agg(sku.code)
	end as skus
from products as p
left join object_links as link on link.left_id = p.id
left join skus as sku on sku.shadow_id = link.right_id
group by p.id;

create unique index product_sku_links_view_idex on product_sku_links_view (product_id);
