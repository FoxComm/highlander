create materialized view store_credit_cancellation_reasons_view as
select
    sc.id,
    -- Subtype
    case when count(r) = 0
    then
        null
    else
        to_json((r.reason_type, r.body)::export_reasons)
    end as canceled_reason  
from store_credits as sc
left join reasons as r on (sc.canceled_reason = r.id)
group by sc.id, r.id;

create unique index store_credit_cancellation_reasons_view_idx on store_credit_cancellation_reasons_view (id);
