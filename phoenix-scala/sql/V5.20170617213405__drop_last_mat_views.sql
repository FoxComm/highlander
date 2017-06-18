-- gift_card_transactions_payments_view

drop materialized view gift_card_transactions_payments_view;

create table gift_card_transactions_payments_view(
    id integer,
    order_payment jsonb,
    scope exts.ltree);

create unique index gift_card_transactions_payments_view_idx on gift_card_transactions_payments_view (id);

insert into gift_card_transactions_payments_view
    select
        gca.id,
        -- Order Payments
        case when op is not null
        then
          to_json((
              o.reference_number,
              to_json_timestamp(o.placed_at),
              to_json_timestamp(op.created_at)
          )::export_order_payments)
        else null end,
        o.scope
    from gift_card_adjustments as gca
    inner join gift_cards as gc on (gca.gift_card_id = gc.id)
    left join order_payments as op on (op.id = gca.order_payment_id)
    left join orders as o on (op.cord_ref = o.reference_number);

-- gift_card_transactions_admins_view

drop materialized view gift_card_transactions_admins_view;

create table gift_card_transactions_admins_view(
    id integer unique,
    store_admin jsonb,
    scope exts.ltree);

insert into gift_card_transactions_admins_view
    select
        gca.id,
        -- Store admins
        case when sa is not null then
          to_json((u.email, u.name)::export_store_admins)
        else null end,
        sa.scope
    from gift_card_adjustments as gca
    inner join gift_cards as gc on (gc.id = gca.gift_card_id)
    left join users as u on (u.account_id = gca.store_admin_id)
    left join admin_data as sa on (sa.account_id = gca.store_admin_id);

--- store_credit_transactions_payments_view

drop materialized view store_credit_transactions_payments_view;

create table store_credit_transactions_payments_view(
    id integer unique,
    order_payment jsonb,
    scope exts.ltree
);

insert into store_credit_transactions_payments_view
  select
    sca.id,
    -- Order Payments
    case when op is not null then
      to_json((
        o.reference_number,
        to_json_timestamp(o.placed_at),
        to_json_timestamp(op.created_at)
      )::export_order_payments)
    else null end,
    o.scope
from store_credit_adjustments as sca
inner join store_credits as sc on (sca.store_credit_id = sc.id)
left join order_payments as op on (op.id = sca.order_payment_id)
left join orders as o on (op.cord_ref = o.reference_number);

-- store_credit_transactions_admins_view

drop materialized view store_credit_transactions_admins_view;

create table store_credit_transactions_admins_view(
  id integer unique,
  store_admin jsonb,
  scope exts.ltree
);

insert into store_credit_transactions_admins_view
  select
    sca.id,
    -- Store admins
    case when sa is not null then
      to_json((u.email, u.name)::export_store_admins)
    else null end,
    sa.scope
  from store_credit_adjustments as sca
  inner join store_credits as sc on (sc.id = sca.store_credit_id)
  left join admin_data as sa on (sa.account_id = sca.store_admin_id)
  left join users as u on (u.account_id = sa.account_id);


-- failed_authorizations_search_view

alter table failed_authorizations_search_view
    rename column charge_id to stripe_charge_id; --  ¯\_(ツ)_/¯

alter table failed_authorizations_search_view
    add column scope exts.ltree;

insert into failed_authorizations_search_view
    select distinct on (ccc.id)
      -- Credit Card Charge
      ccc.id,
      ccc.stripe_charge_id,
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
      o.reference_number as cord_reference_number,
      -- Customer
      o.account_id as account_id,
      -- Scope
      o.scope
    from credit_card_charges as ccc
      inner join credit_cards as cc on (ccc.credit_card_id = cc.id)
      inner join regions as r on (cc.region_id = r.id)
      inner join countries as c on (r.country_id = c.id)
      inner join order_payments as op on (op.id = ccc.order_payment_id)
      inner join orders as o on (op.cord_ref = o.reference_number)
    where ccc.state = 'failedAuth'
    order by ccc.id
  on conflict (id) do nothing ; -- ignore if already exists