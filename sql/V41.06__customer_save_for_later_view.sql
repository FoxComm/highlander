create materialized view customer_save_for_later_view as
select
    c.id as customer_id,
    count(sku.id) as count,
    case when count(sku) = 0
    then
        '[]'
    else
        json_agg((sku.code, 
                sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref'), 
                sku_form.attributes->(sku_shadow.attributes->'price'->>'ref')->>'value')::export_skus)
    end as items  
from customers as c
left join save_for_later as later on (c.id = later.customer_id)
left join skus as sku on (later.sku_id = sku.id)
left join object_forms as sku_form on (sku.form_id = sku_form.id)
left join object_shadows as sku_shadow on (sku.shadow_id = sku_shadow.id)
group by c.id;

create unique index customer_save_for_later_view_idx on customer_save_for_later_view (customer_id);
