create domain money_amount bigint;

-- drop depended objects
drop materialized view failed_authorizations_search_view;
drop trigger if exists update_orders_view_from_customers_ranking_on_returns_payments on return_payments;
drop materialized view gift_card_transactions_view;

alter table credit_card_charges alter column amount type money_amount;
alter table store_credits alter column canceled_amount type money_amount;
alter table store_credits alter column available_balance type money_amount;
alter table store_credits alter column current_balance type money_amount;
alter table store_credits alter column original_balance type money_amount;
alter table store_credits_search_view alter column canceled_amount type money_amount;
alter table store_credits_search_view alter column available_balance type money_amount;
alter table store_credits_search_view alter column current_balance type money_amount;
alter table store_credits_search_view alter column original_balance type money_amount;
alter table order_payments alter column amount type money_amount;
alter table return_cc_payments alter column amount type money_amount;
alter table gift_cards alter column canceled_amount type money_amount;
alter table gift_cards alter column original_balance type money_amount;
alter table gift_cards alter column current_balance type money_amount;
alter table gift_cards alter column available_balance type money_amount;
alter table gift_cards_search_view alter column canceled_amount type money_amount;
alter table gift_cards_search_view alter column current_balance type money_amount;
alter table gift_cards_search_view alter column available_balance type money_amount;
alter table gift_cards_search_view alter column original_balance type money_amount;
alter table return_payments alter column amount type money_amount;
alter table return_line_item_shipping_costs alter column amount type money_amount;
alter table customers_search_view alter column revenue type money_amount;
alter table order_shipping_methods alter column price type money_amount;
alter table shipping_methods alter column price type money_amount;
alter table gift_card_adjustments alter column debit type money_amount;
alter table gift_card_adjustments alter column credit type money_amount;
alter table gift_card_adjustments alter column available_balance type money_amount;
alter table store_credit_adjustments alter column available_balance type money_amount;
alter table store_credit_adjustments alter column debit type money_amount;
alter table orders alter column adjustments_total type money_amount;
alter table orders alter column taxes_total type money_amount;
alter table orders alter column grand_total type money_amount;
alter table orders alter column shipping_total type money_amount;
alter table orders alter column sub_total type money_amount;
alter table carts alter column adjustments_total type money_amount;
alter table carts alter column taxes_total type money_amount;
alter table carts alter column grand_total type money_amount;
alter table carts alter column shipping_total type money_amount;
alter table carts alter column sub_total type money_amount;
alter table cart_line_item_adjustments alter column subtract type money_amount;
alter table returns alter column total_refund type money_amount;
alter table shipments alter column shipping_price type money_amount;

-- restore depended objects
create table failed_authorizations_search_view as
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
    o.reference_number as cord_reference_number,
    -- Customer
    o.account_id as account_id
  from credit_card_charges as ccc
    inner join credit_cards as cc on (ccc.credit_card_id = cc.id)
    inner join regions as r on (cc.region_id = r.id)
    inner join countries as c on (r.country_id = c.id)
    inner join order_payments as op on (op.id = ccc.order_payment_id)
    inner join orders as o on (op.cord_ref = o.reference_number)
  where ccc.state = 'failedAuth'
  order by ccc.id;

create unique index failed_authorizations_search_view_idx on failed_authorizations_search_view (id);


create trigger update_orders_view_from_customers_ranking_on_returns_payments
after update or insert on return_payments
for each row
when (new.amount is not null)
execute procedure update_orders_view_from_customers_ranking_fn();


create table gift_card_transactions_view as
  select distinct on (gca.id)
    -- Gift Card Transaction
    gca.id,
    gca.debit,
    gca.credit,
    gca.available_balance,
    gca.state,
    to_char(gca.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Gift Card
    gc.code,
    gc.origin_type,
    gc.currency,
    to_char(gc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as gift_card_created_at,
    -- Order Payment
    gctpv.order_payment,
    -- Store admins
    gctav.store_admin
  from gift_card_adjustments as gca
    inner join gift_cards as gc on (gc.id = gca.gift_card_id)
    inner join gift_card_transactions_payments_view as gctpv on (gctpv.id = gca.id)
    inner join gift_card_transactions_admins_view as gctav on (gctav.id = gca.id)
  order by gca.id;

create unique index gift_card_transactions_view_idx on gift_card_transactions_view (id);
