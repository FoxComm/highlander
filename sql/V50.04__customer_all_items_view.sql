create sequence customer_all_items;

create materialized view customer_all_items_view as
select 
	nextval('customer_all_items') as id,
	'purchased' as type,
	customer_id,
	customer_name,
	customer_email,
	sku_code,
	sku_title,
	sku_price,
	order_reference_number,
	order_placed_at
from
	customer_purchased_items_view
union all
select 
	nextval('customer_all_items') as id,
	'save_for_later' as type,
	customer_id,
	customer_name,
	customer_email,
	sku_code,
	sku_title,
	sku_price,
	null as order_reference_number,
	null as order_placed_at		
from
	customer_save_for_later_view;

create unique index customer_all_items_view_idx on customer_all_items_view (id);