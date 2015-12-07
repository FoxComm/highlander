create materialized view customer_store_credit_view as
select
    c.id as customer_id,
    count(sc.id) as count,
    coalesce(sum(sc.available_balance), 0) as total
from customers as c
left join store_credits as sc on c.id = sc.customer_id
group by c.id;

create unique index customer_store_credit_view_idx on customer_store_credit_view (customer_id);