create materialized view notes_search_view as
select distinct on (n.id)
    -- Note
    n.id as id,
    n.reference_id as reference_id,
    n.reference_type as reference_type,
    n.body as body,
    n.priority as priority,
    to_char(n.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    to_char(n.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as deleted_at,
    admins.store_admin as author,
    -- OneOf optional entity
    orders.order,
    customers.customer,
    gift_cards.gift_card,
    skus.sku as sku_item,
    products.product,
    promotions.promotion,
    coupons.coupon
from notes as n
inner join notes_admins_view as admins on (n.id = admins.id)
inner join notes_orders_view as orders on (n.id = orders.id)
inner join notes_customers_view as customers on (n.id = customers.id)
inner join notes_gift_cards_view as gift_cards on (n.id = gift_cards.id)
inner join notes_skus_view as skus on (n.id = skus.id)
inner join notes_products_view as products on (n.id = products.id)
inner join notes_promotions_view as promotions on (n.id = promotions.id)
inner join notes_coupons_view as coupons on (n.id = coupons.id)
order by id;

create unique index notes_search_view_idx on notes_search_view (id);
