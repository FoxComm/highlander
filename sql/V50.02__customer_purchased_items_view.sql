create materialized view customer_purchased_items_view as
select
    oli.id,
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
    s.attributes->'title'->>(ss.attributes->>'title') as sku_title,
    s.attributes->'price'->(ss.attributes->>'price')->>'value' as sku_price
from order_line_item_skus as oli_skus
inner join order_line_item_origins as oli_origins on oli_origins.id = oli_skus.id
inner join order_line_items as oli on oli.origin_id = oli_origins.id and oli.state = 'shipped'
inner join orders as o on o.id = oli.order_id and o.state = 'shipped'
inner join customers as c on o.customer_id = c.id
inner join skus as s on oli_skus.sku_id = s.id
inner join sku_shadows as ss on oli_skus.sku_shadow_id = ss.id;

create unique index customer_purchased_items_view_idx on customer_purchased_items_view (id);
