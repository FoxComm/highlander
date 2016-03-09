create materialized view customer_save_for_later_view as
select
    later.id,
    -- Customer
    later.customer_id as customer_id,
    c.name as customer_name,
    c.email as customer_email,
    -- SKU
    s.code as sku_code,
    s.attributes->'title'->>(ss.attributes->>'title') as sku_title,
    s.attributes->'price'->(ss.attributes->>'price')->>'value' as sku_price
from save_for_later as later
inner join customers as c on later.customer_id = c.id
inner join skus as s on later.sku_id = s.id
inner join sku_shadows as ss on later.sku_shadow_id = ss.id;

create unique index customer_save_for_later_view_idx on customer_save_for_later_view (id);
