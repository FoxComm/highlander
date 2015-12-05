-- depends on customers, orders, orders_payments with childs and rma

create materialized view customers_ranking as
select ranking.id,
  ranking.revenue,
  ntile(100) over (w) as rank
from
  (select
      c.id,
      coalesce(sum(CCc.amount),0) + coalesce(sum(SCa.debit), 0) + coalesce(sum(GCa.debit),0) - coalesce(sum(rp.amount),0) as revenue
    from customers as c
    inner join orders on(c.id = orders.customer_id and orders.status = 'shipped')
    inner join order_payments as op on(op.order_id = orders.id)
    left join credit_card_charges as CCc on(CCc.order_payment_id = op.id and CCc.status = 'fullCapture')
    left join store_credit_adjustments as SCa on(SCA.order_payment_id = op.id and SCa.status = 'capture')
    left join gift_card_adjustments as GCa on (GCa.order_payment_id = op.id and GCa.status = 'capture')
    left join rmas on(rmas.order_id = orders.id and rmas.status = 'complete')
    left join rma_payments as rp on (rp.rma_id = rmas.id and rp.amount is not null)
    where is_guest = false
    group by (c.id)
    order by revenue desc
  ) as ranking
where revenue > 0
window w as (order by ranking.revenue desc)
order by revenue desc;

create unique index customers_ranking__id_idx on customers_ranking (id);
create index customers_ranking__rank_idx on customers_ranking (rank);