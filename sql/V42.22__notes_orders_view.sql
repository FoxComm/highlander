create materialized view notes_orders_view as
select
    n.id,
    -- Order
    case when count(o) = 0
    then
        null
    else
        to_json((
        	o.customer_id,
        	o.reference_number,
        	o.state,
        	to_char(o.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        	to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        	o.sub_total,
        	o.shipping_total,
        	o.adjustments_total,
        	o.taxes_total,
        	o.grand_total
        )::export_orders)
    end as order
from notes as n
left join orders as o on (n.reference_id = o.id AND n.reference_type = 'order')
group by n.id, o.id;

create unique index notes_orders_view_idx on notes_orders_view (id);
