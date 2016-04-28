create materialized view notes_orders_view as
select
    n.id,
    -- Order
    case when count(osv) = 0
    then
        '[]'
    else
        json_agg((
        	osv.customer_id,
        	osv.reference_number,
        	osv.state,
        	to_char(osv.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        	to_char(osv.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        	osv.sub_total,
        	osv.shipping_total,
        	osv.adjustments_total,
        	osv.taxes_total,
        	osv.grand_total,
        	osv.items_count
        )::export_orders)
    end as order
from notes as n
left join order_stats_view as osv on (n.reference_id = osv.id AND n.reference_type = 'order')
group by n.id;

create unique index notes_orders_view_idx on notes_orders_view (id);
