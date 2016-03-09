create sequence customer_items_view_seq;

create materialized view customer_items_view as
select
	nextval('customer_items_view_seq') as id,
	-- Customer
	coalesce(t1.customer_id, t2.customer_id) as customer_id,
	coalesce(t1.customer_name, t2.customer_name) as customer_name,
	coalesce(t1.customer_email, t2.customer_email) as customer_email,
	-- SKU
	coalesce(t1.sku_code, t2.sku_code) as sku_code,
	coalesce(t1.sku_title, t2.sku_title) as sku_title,
	coalesce(t1.sku_price, t2.sku_price) as sku_price,
	-- Order
	coalesce(t1.order_reference_number, null) as order_reference_number,
	coalesce(t1.order_placed_at, null) as order_placed_at,
	-- Save for later
	coalesce(null, t2.saved_for_later_at) as saved_for_later_at	
from customer_purchased_items_view as t1
full outer join customer_save_for_later_view as t2 ON t1.id = t2.id;


create unique index customer_items_view_idx on customer_items_view (id);