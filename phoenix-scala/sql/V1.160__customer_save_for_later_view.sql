create materialized view customer_save_for_later_view as
select
    s.id,
    -- Customer
    later.customer_id as customer_id,
    c.name as customer_name,
    c.email as customer_email,
    -- SKU
    s.code as sku_code,
    f.attributes->>(sh.attributes->'title'->>'ref') as sku_title,
    f.attributes->(sh.attributes->'salePrice'->>'ref')->>'value' as sku_price,
    -- Save for later
    to_char(later.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as saved_for_later_at
from save_for_later as later
inner join customers as c on later.customer_id = c.id
inner join skus as s on later.sku_id = s.id
inner join object_forms as f on f.id = s.form_id
inner join object_shadows as sh on sh.id = s.shadow_id;

create unique index customer_save_for_later_view_idx on customer_save_for_later_view (id, customer_id);
