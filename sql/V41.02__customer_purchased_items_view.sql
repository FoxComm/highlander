create materialized view customer_purchased_items_view as
select
    c.id as customer_id,
    count(s.id) as count,
    case when count(s) = 0
    then
        '[]'
    else
        json_agg((s.sku, s.name, s.price)::export_skus)
    end as items
from customers as c
left join orders as o2 on c.id = o2.customer_id and o2.status = 'shipped'
left join order_line_items as oli on o2.id = oli.order_id and oli.status = 'shipped'
left join order_line_item_origins as oli_origins on oli.origin_id = oli_origins.id
left join order_line_item_skus as oli_skus on oli_origins.id = oli_skus.id
left join skus as s on oli_skus.sku_id = s.id
group by c.id;

create unique index customer_purchased_items_view_idx on customer_purchased_items_view (customer_id);