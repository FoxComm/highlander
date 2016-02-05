create materialized view store_credit_from_gift_cards_view as
select
    sc.id,
    -- Gift cards
    case when count(gc) = 0
    then
        null
    else
        to_json((gc.code, gc.origin_type, gc.currency, to_char(gc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_gift_cards)
    end as gift_card  
from store_credits as sc
left join store_credit_from_gift_cards as scfgc on (sc.origin_id = scfgc.id)
left join gift_cards as gc on (gc.id = scfgc.gift_card_id)
group by sc.id, gc.id;

create unique index store_credit_from_gift_cards_view_idx on store_credit_from_gift_cards_view (id);
