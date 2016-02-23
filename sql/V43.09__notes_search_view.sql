create materialized view notes_search_view as
select distinct on (n.id)
    -- Note
    n.id as id,
    n.reference_type as reference_type,
    n.body as body,
    n.priority as priority,
    to_char(n.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    to_char(n.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as deleted_at,
    nav.store_admin as author,
    -- Order
    nov.order,
    -- Customer
    ncv.customer,
    -- Gift Card
    ngcv.gift_card
from notes as n
inner join notes_admins_view as nav on (n.id = nav.id)
inner join notes_orders_view as nov on (n.id = nov.id)
inner join notes_customers_view as ncv on (n.id = ncv.id)
inner join notes_gift_cards_view as ngcv on (n.id = ngcv.id)
order by n.id;

create unique index notes_search_view_idx on notes_search_view (id);
