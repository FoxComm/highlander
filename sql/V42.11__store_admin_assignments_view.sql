create materialized view store_admin_assignments_view as
select
    s.id as store_admin_id,
    count(oa.id) as count,
    case when count(o) = 0
    then
        '[]'
    else
        json_agg((o.reference_number, to_char(oa.created_at, 'YYYY-MM-DD HH24:MI:SS'))::export_assignments)
    end as assignments
from store_admins as s
left join order_assignments as oa on (s.id = oa.assignee_id)
left join orders as o on (oa.order_id = o.id)
group by s.id;

create unique index store_admin_assignments_view_idx on store_admin_assignments_view (store_admin_id);
