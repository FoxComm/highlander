create materialized view customer_purchased_items_view as
select
    s.id,
    oli.reference_number,
    to_char(oli.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Order
    o.reference_number as order_reference_number,
    to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as order_placed_at,
    -- Customer
    o.customer_id as customer_id,
    c.name as customer_name,
    c.email as customer_email,
    -- SKU
    s.code as sku_code,
    f.attributes->>(sh.attributes->'title'->>'ref') as sku_title,
    f.attributes->(sh.attributes->'price'->>'ref')->>'value' as sku_price
from order_line_item_skus as oli_skus
inner join order_line_item_origins as oli_origins on oli_origins.id = oli_skus.id
inner join order_line_items as oli on oli.origin_id = oli_origins.id and oli.state = 'shipped'
inner join orders as o on o.id = oli.order_id and o.state = 'shipped'
inner join customers as c on o.customer_id = c.id
inner join skus as s on oli_skus.sku_id = s.id
inner join object_forms as f on f.id = s.form_id
inner join object_shadows as sh on sh.id = s.shadow_id;

create unique index customer_purchased_items_view_idx on customer_purchased_items_view (id, customer_id);
