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
left join sku_product_links as link on p.id = link.product_id
left join skus as sku on sku.id = link.sku_id
group by p.id;

create unique index product_sku_links_view_idex on product_sku_links_view (product_id);
