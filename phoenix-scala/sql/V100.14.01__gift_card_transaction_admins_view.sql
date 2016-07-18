create materialized view gift_card_transactions_admins_view as
select
    gca.id,
    -- Store admins
    case when count(sa) = 0
    then
        null
    else
        to_json((sa.email, sa.name, sa.department)::export_store_admins)
    end as store_admin
from gift_card_adjustments as gca
inner join gift_cards as gc on (gc.id = gca.gift_card_id)
left join store_admins as sa on (sa.id = gca.store_admin_id)
group by gca.id, sa.id;

create unique index gift_card_transactions_admins_view_idx on gift_card_transactions_admins_view (id);
