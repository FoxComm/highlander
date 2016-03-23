create materialized view store_admins_search_view as
select distinct on (s.id)
    -- Store Admin
    s.id as id,
    s.email as email,
    s.name as name,
    s.department as department,
    to_char(s.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at
from store_admins as s
order by s.id;

create unique index store_admins_search_view_idx on store_admins_search_view (id);
