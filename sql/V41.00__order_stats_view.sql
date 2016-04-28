create materialized view order_stats_view as
select
	o.id,
    o.customer_id,
	o.reference_number,
	o.state,
	o.created_at,
	o.placed_at,
	o.sub_total,
	o.shipping_total,
	o.adjustments_total,
	o.taxes_total,
	o.grand_total,
    count(oli.id) as items_count
from orders as o
left join order_line_items as oli on o.id = oli.order_id
group by o.id;

create unique index order_stats_view_idx on order_stats_view (id);
