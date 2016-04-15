create materialized view notes_admins_view as
select
    n.id,
    -- Store admin
    case when count(sa) = 0
    then
        null
    else
        to_json((sa.email, sa.name, sa.department)::export_store_admins)
    end as store_admin
from notes as n
inner join store_admins as sa on (n.store_admin_id = sa.id)
group by n.id, sa.id;

create unique index notes_admins_view_idx on notes_admins_view (id);
