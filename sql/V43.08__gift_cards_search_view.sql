create materialized view gift_cards_search_view as
select distinct on (gc.id)
    -- Gift card
    gc.id,
    gc.code,
    gc.origin_id,
    gc.origin_type,
    gcsv.subtype,
    gc.state,
    gc.currency,
    gc.original_balance,
    gc.current_balance,
    gc.available_balance,
    gc.canceled_amount,
    gccrv.canceled_reason,
    to_char(gc.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at,
    to_char(gc.updated_at, 'YYYY-MM-DD HH24:MI:SS') as updated_at,
    -- Issued by either
    gcav.store_admin,
    gcfscv.store_credit
from gift_cards as gc
inner join gift_card_subtypes_view as gcsv on (gcsv.id = gc.id)
inner join gift_card_cancellation_reasons_view as gccrv on (gccrv.id = gc.id)
inner join gift_card_admins_view as gcav on (gcav.id = gc.id)
inner join gift_card_from_store_credits_view as gcfscv on (gcfscv.id = gc.id)
order by gc.id;

create unique index gift_cards_search_view_idx on gift_cards_search_view (id);
