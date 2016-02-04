create materialized view store_credit_transactions_admins_view as
select
    sca.id,
    -- Store admins
    case when count(sa) = 0
    then
        null
    else
        to_json((sa.email, sa.name, sa.department)::export_store_admins)
    end as store_admin 
from store_credit_adjustments as sca
inner join store_credits as sc on (sc.id = sca.store_credit_id)
left join store_admins as sa on (sa.id = sca.store_admin_id)
group by sca.id, sa.id;

create unique index store_credit_transactions_admins_view_idx on store_credit_transactions_admins_view (id);
