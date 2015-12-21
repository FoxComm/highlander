create materialized view store_admins_search_view as
select distinct on (s.id)
    -- Store Admin
    s.id as id,
    s.email as email,
    s.first_name as first_name,
    s.last_name as last_name,
    s.department as department,
    to_char(s.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at,
    -- Assignments
    a.count as assignments_count,
    a.assignments as assignments
from store_admins as s
inner join store_admin_assignments_view as a on (s.id = a.store_admin_id)
order by s.id;

create unique index store_admins_search_view_idx on store_admins_search_view (id);
