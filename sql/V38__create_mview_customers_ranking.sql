-- depends on customers, orders, orders_payments with childs and rma
-- FIXME when implementing shipping! Revert https://github.com/FoxComm/phoenix-scala/pull/908

create materialized view customers_ranking as
select ranking.id,
  ranking.revenue,
  ntile(100) over (w) as rank
from
  (select
      c.id,
      coalesce(sum(CCc.amount),0) + coalesce(sum(SCa.debit), 0) + coalesce(sum(GCa.debit),0) - coalesce(sum(rp.amount),0) as revenue
    from customers as c
    inner join orders on(c.id = orders.customer_id and orders.state in('remorseHold', 'fulfillmentStarted', 'shipped'))
    inner join order_payments as op on(op.order_ref = orders.reference_number)
    left join credit_card_charges as CCc on(CCc.order_payment_id = op.id and CCc.state in ('auth', 'fullCapture'))
    left join store_credit_adjustments as SCa on(SCA.order_payment_id = op.id and SCa.state in ('auth', 'capture'))
    left join gift_card_adjustments as GCa on (GCa.order_payment_id = op.id and GCa.state in ('auth', 'capture'))
    left join returns on (returns.order_ref = orders.reference_number and returns.state = 'complete')
    left join return_payments as rp on (rp.return_id = returns.id and rp.amount is not null)
    where is_guest = false
    group by (c.id)
    order by revenue desc
  ) as ranking
where revenue > 0
window w as (order by ranking.revenue desc)
order by revenue desc;

create unique index customers_ranking__id_idx on customers_ranking (id);
create index customers_ranking__rank_idx on customers_ranking (rank);