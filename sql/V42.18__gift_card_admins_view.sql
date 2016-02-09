create materialized view gift_card_admins_view as
select
    gc.id,
    -- Store admins
    case when count(sa) = 0
    then
        null
    else
        to_json((sa.email, sa.name, sa.department)::export_store_admins)
    end as store_admin
from gift_cards as gc
left join gift_card_manuals as gcm on (gc.origin_id = gcm.id)
left join store_admins as sa on (sa.id = gcm.admin_id)
group by gc.id, sa.id;

create unique index gift_card_admins_view_idx on gift_card_admins_view (id);
