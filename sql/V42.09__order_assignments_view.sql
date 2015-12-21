create materialized view order_assignments_view as
select
    o.id as order_id,
    count(oa.id) as count,
    case when count(sa) = 0
    then
        '[]'
    else
        json_agg((sa.first_name, sa.last_name, to_char(oa.created_at, 'YYYY-MM-dd'))::export_assignees)
    end as assignees
from orders as o
left join order_assignments as oa on (o.id = oa.order_id)
left join store_admins as sa on (sa.id = oa.assignee_id)
group by o.id;

create unique index order_assignments_view_idx on order_assignments_view (order_id);
