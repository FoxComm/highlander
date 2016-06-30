create materialized view failed_authorizations_search_view as
select distinct on (ccc.id)
    -- Credit Card Charge
    ccc.id,
    ccc.charge_id,
    ccc.amount,
    ccc.currency,
    ccc.state,
    to_char(ccc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Credit Card
    cc.holder_name,
    cc.last_four,
    cc.exp_month,
    cc.exp_year,
    cc.brand,
    -- Billing address
    cc.address1,
    cc.address2,
    cc.city,
    cc.zip,
    r.name as region,
    c.name as country,
    c.continent,
    -- Order
    o.reference_number as order_reference_number,
    -- Customer
    o.customer_id as customer_id
from credit_card_charges as ccc
inner join credit_cards as cc on (ccc.credit_card_id = cc.id)
inner join regions as r on (cc.region_id = r.id)
inner join countries as c on (r.country_id = c.id)
inner join order_payments as op on (op.id = ccc.order_payment_id)
inner join orders as o on (op.order_ref = o.reference_number)
where ccc.state = 'failedAuth'
order by ccc.id;

create unique index failed_authorizations_search_view_idx on failed_authorizations_search_view (id);
