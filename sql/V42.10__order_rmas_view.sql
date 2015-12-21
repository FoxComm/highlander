create materialized view order_rmas_view as
select
    o.id as order_id,
    count(rmas.id) as count,
    case when count(rmas) = 0
    then
        '[]'
    else
        json_agg((rmas.reference_number, rmas.status, rmas.rma_type, to_char(rmas.created_at, 'YYYY-MM-DD HH24:MI:SS'))::export_rmas)
    end as rmas
from orders as o
left join rmas on (o.id = rmas.order_id)
group by o.id;

create unique index order_rmas_view_idx on order_rmas_view (order_id);
