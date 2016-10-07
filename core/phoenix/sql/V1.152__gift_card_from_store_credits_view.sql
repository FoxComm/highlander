create materialized view gift_card_from_store_credits_view as
select
    gc.id,
    -- Gift cards
    case when count(sc) = 0
    then
        null
    else
        to_json((sc.id, sc.customer_id, sc.origin_type, sc.currency, to_char(sc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_store_credits)
    end as store_credit
from gift_cards as gc
left join gift_card_from_store_credits as gcfsc on (gc.origin_id = gcfsc.id)
left join store_credits as sc on (sc.id = gcfsc.store_credit_id)
group by gc.id, sc.id;

create unique index gift_card_from_store_credits_view_idx on gift_card_from_store_credits_view (id);
