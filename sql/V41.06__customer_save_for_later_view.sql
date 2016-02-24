create materialized view customer_save_for_later_view as
select
    c.id as customer_id,
    count(sku_later.id) as count,
    case when count(sku_later) = 0
    then
        '[]'
    else
        json_agg((sku_later.code, 
                sku_later.attributes->'title'->>(sku_shadow.attributes->>'title'), 
                sku_later.attributes->'price'->(sku_shadow.attributes->>'price')->>'value')::export_skus)
    end as items  
from customers as c
left join save_for_later as later on (c.id = later.customer_id)
left join skus as sku_later on (later.sku_id = sku_later.id)
left join sku_shadows as sku_shadow on (later.sku_shadow_id = sku_shadow.id)
group by c.id;

create unique index customer_save_for_later_view_idx on customer_save_for_later_view (customer_id);
