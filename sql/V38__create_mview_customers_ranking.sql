-- depends on customers, orders, orders_payments and rma

create materialized view customers_ranking as
select
	c.id,
	sum(p.amount) - coalesce(sum(rp.amount),0) as revenue,
	ntile(100) over (w) as rank
from customers as c
inner join orders on(c.id = orders.customer_id and orders.status = 'shipped')
inner join order_payments as p on(p.order_id = orders.id and p.amount is not null)
left join rmas on(rmas.order_id = orders.id and rmas.status = 'complete')
left join rma_payments as rp on (rp.rma_id = rmas.id and rp.amount is not null)
group by (c.id)
window w as (order by sum(p.amount) desc)
order by revenue desc;

create unique index customers_ranking__id_idx on customers_ranking (id);
create index customers_ranking__rank_idx on customers_ranking (rank);