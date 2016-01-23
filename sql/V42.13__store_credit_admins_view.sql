create materialized view store_credit_admins_view as
select
    sc.id,
    -- Store admins
    case when count(sa) = 0
    then
        null
    else
        to_json((sa.email, sa.name, sa.department)::export_store_admins)
    end as store_admin  
from store_credits as sc
left join store_credit_manuals as scm on (sc.origin_id = scm.id)
left join store_admins as sa on (sa.id = scm.admin_id)
group by sc.id, sa.id;

create unique index store_credit_admins_view_idx on store_credit_admins_view (id);
