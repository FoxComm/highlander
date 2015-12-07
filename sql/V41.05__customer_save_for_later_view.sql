create materialized view customer_save_for_later_view as
select
    c.id as customer_id,
    count(sku_later.id) as count,
    case when count(sku_later) = 0
    then
        '[]'
    else
        json_agg((sku_later.sku, sku_later.name, sku_later.price)::export_skus)
    end as items  
from customers as c
left join save_for_later as later on (c.id = later.customer_id)
left join skus as sku_later on (later.sku_id = sku_later.id)
group by c.id;

create unique index customer_save_for_later_view_idx on customer_save_for_later_view (customer_id);