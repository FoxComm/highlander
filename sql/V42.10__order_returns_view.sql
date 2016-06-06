create materialized view order_returns_view as
select
    o.id as order_id,
    count(r.id) as count,
    case when count(r) = 0
    then
        '[]'
    else
        json_agg((r.reference_number, r.state, r.return_type, to_char(r.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_returns)
    end as returns
from orders as o
left join returns as r on (o.id = r.order_id)
group by o.id;

create unique index order_returns_view_idx on order_returns_view (order_id);
