create materialized view store_credit_subtypes_view as
select
    sc.id,
    -- Subtype
    case when count(scs) = 0
    then
        null
    else
        scs.title
    end as subtype  
from store_credits as sc
left join store_credit_subtypes as scs on (sc.subtype_id = scs.id)
group by sc.id, scs.id;

create unique index store_credit_subtypes_view_idx on store_credit_subtypes_view (id);
