create materialized view gift_card_cancellation_reasons_view as
select
    gc.id,
    -- Subtype
    case when count(r) = 0
    then
        null
    else
        to_json((r.reason_type, r.body)::export_reasons)
    end as canceled_reason
from gift_cards as gc
left join reasons as r on (gc.canceled_reason = r.id)
group by gc.id, r.id;

create unique index gift_card_cancellation_reasons_view_idx on gift_card_cancellation_reasons_view (id);
