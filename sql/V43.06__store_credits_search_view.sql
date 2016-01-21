create materialized view store_credits_search_view as
select distinct on (sc.id)
    -- Store Credit
    sc.id,
    sc.customer_id,
    sc.origin_id,
    sc.origin_type,
    scsv.subtype,
    sc.status,    
    sc.currency,
    sc.original_balance,
    sc.current_balance,
    sc.available_balance,
    sc.canceled_amount,
    sccrv.canceled_reason,
    to_char(sc.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at,
    to_char(sc.updated_at, 'YYYY-MM-DD HH24:MI:SS') as updated_at,
    -- Issued by either
    scav.store_admin,
    scfgcv.gift_card
from store_credits as sc
inner join store_credit_subtypes_view as scsv on (scsv.id = sc.id)
inner join store_credit_cancellation_reasons_view as sccrv on (sccrv.id = sc.id)
inner join store_credit_admins_view as scav on (scav.id = sc.id)
inner join store_credit_from_gift_cards_view as scfgcv on (scfgcv.id = sc.id)
order by sc.id;

create unique index store_credits_search_view_idx on store_credits_search_view (id);
