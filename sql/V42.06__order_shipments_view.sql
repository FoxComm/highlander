create materialized view order_shipments_view as
select
    o.id as order_id,
    count(shipments.id) as count,
    case when count(shipments) = 0
    then
        '[]'
    else
        json_agg((shipments.status, shipments.shipping_price, sm.admin_display_name, sm.storefront_display_name)::export_shipments)
    end as shipments
from orders as o
left join shipments on (o.id = shipments.order_id)
left join shipping_methods as sm on (shipments.order_shipping_method_id = sm.id)
group by o.id;

create unique index order_shipments_view_idx on order_shipments_view (order_id);