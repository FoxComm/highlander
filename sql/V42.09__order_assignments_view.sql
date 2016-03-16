create materialized view order_assignments_view as
select
    o.id as order_id,
    count(a.id) as count,
    case when count(sa) = 0
    then
        '[]'
    else
        json_agg((sa.name, to_char(a.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_assignees)
    end as assignees
from orders as o
left join assignments as a on (o.id = a.reference_id and a.reference_type = 'order')
left join store_admins as sa on (sa.id = a.store_admin_id)
group by o.id;

create unique index order_assignments_view_idx on order_assignments_view (order_id);