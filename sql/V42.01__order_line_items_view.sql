create materialized view order_line_items_view as
select
    o.id as order_id,
    count(s.id) as count,
    case when count(s) = 0
    then
        '[]'
    else
        json_agg((oli.status, s.sku, s.name, s.price)::export_line_items)
    end as items
from orders as o
left join order_line_items as oli on (o.id = oli.order_id)
left join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
left join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
left join skus as s on (oli_skus.sku_id = s.id)
group by o.id;

create unique index order_line_items_view_idx on order_line_items_view (order_id);