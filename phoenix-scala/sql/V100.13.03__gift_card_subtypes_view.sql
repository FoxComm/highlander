create materialized view gift_card_subtypes_view as
select
    gc.id,
    -- Subtype
    case when count(gcs) = 0
    then
        null
    else
        gcs.title
    end as subtype
from gift_cards as gc
left join gift_card_subtypes as gcs on (gc.subtype_id = gcs.id)
group by gc.id, gcs.id;

create unique index gift_card_subtypes_view_idx on gift_card_subtypes_view (id);
