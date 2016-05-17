create or replace function update_orders_view_from_payments_fn() returns trigger as $$
declare order_ids int[];
begin
  case TG_TABLE_NAME
    when 'order_payments' then
      order_ids := array_agg(NEW.order_id);
    when 'credit_card_charges' then
      select array_agg(op.order_id) into strict order_ids
        from credit_card_charges as ccp
        inner join order_payments as op on (op.id = ccp.order_payment_id)
        WHERE ccp.id = NEW.id;
    when 'gift_card_adjustments' then
      select array_agg(op.order_id) into strict order_ids
        from gift_card_adjustments as gcc
        inner join order_payments as op on (op.id = gcc.order_payment_id)
        WHERE gcc.id = NEW.id;
    when 'store_credit_adjustments' then
      select array_agg(op.order_id) into strict order_ids
        from store_credit_adjustments as sca
        inner join order_payments as op on (op.id = sca.order_payment_id)
        WHERE sca.id = NEW.id;
  end case;

  update orders_search_view set
    payments = subquery.payments
      from (select
      o.id,
      case when count(op) = 0
      then
          '[]'
      else
          json_agg((op.payment_method_type, op.amount, op.currency, ccp.state, gcc.state, sca.state)::export_payments)
      end as payments
      from orders as o
      left join order_payments as op on (o.id = op.order_id)
      left join credit_card_charges as ccp on (op.id = ccp.order_payment_id)
      left join gift_card_adjustments as gcc on (op.id = gcc.order_payment_id)
      left join store_credit_adjustments as sca on (op.id = sca.order_payment_id)
      where o.id = ANY(order_ids)
      group by o.id) AS subquery
    WHERE orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;


create or replace function update_orders_from_payments_creditCard_fn() returns trigger as $$
begin
  update orders_search_view set
    credit_card_count = subquery.credit_card_count,
    credit_card_total = subquery.credit_card_total
    FROM (
      SELECT
        op.order_id,
        count(op.id) as credit_card_count,
        coalesce(sum(op.amount), 0) as credit_card_total
        from order_payments as op
        WHERE op.id = NEW.id AND op.payment_method_type = 'creditCard'
        group by op.order_id
      ) AS subquery
  WHERE orders_search_view.id = subquery.order_id;
  return NULL;
END;
$$ LANGUAGE plpgsql;

create or replace function update_orders_from_payments_giftCard_fn() returns trigger as $$
begin
  update orders_search_view set
    gift_card_count = subquery.gift_card_count,
    gift_card_total = subquery.gift_card_total
    FROM (
      SELECT
        op.order_id,
        count(op.id) as gift_card_count,
        coalesce(sum(op.amount), 0) as gift_card_total
        from order_payments as op
        WHERE op.id = NEW.id AND op.payment_method_type = 'giftCard'
        group by op.order_id
      ) AS subquery
  WHERE orders_search_view.id = subquery.order_id;
  return NULL;
END;
$$ LANGUAGE plpgsql;

create or replace function update_orders_from_payments_storeCredit_fn() returns trigger as $$
begin
  update orders_search_view set
    store_credit_count = subquery.store_credit_count,
    store_credit_total = subquery.store_credit_total
    FROM (
      SELECT
        op.order_id,
        count(op.id) as store_credit_count,
        coalesce(sum(op.amount), 0) as store_credit_total
        from order_payments as op
        WHERE op.id = NEW.id AND op.payment_method_type = 'storeCredit'
        group by op.order_id
      ) AS subquery
  WHERE orders_search_view.id = subquery.order_id;
  return NULL;
END;
$$ LANGUAGE plpgsql;


create trigger update_orders_view_from_payments
    after update or insert on order_payments
    for each row
    execute procedure update_orders_view_from_payments_fn();

create trigger update_orders_view_from_payments_creditCards
  after update or insert on order_payments
  for each row
  when (NEW.payment_method_type = 'creditCard')
  execute procedure update_orders_from_payments_creditCard_fn();

create trigger update_orders_view_from_payments_giftCards
  after update or insert on order_payments
  for each row
  when (NEW.payment_method_type = 'giftCard')
  execute procedure update_orders_from_payments_giftCard_fn();

create trigger update_orders_view_from_payments_storeCredits
  after update or insert on order_payments
  for each row
  when (NEW.payment_method_type = 'storeCredit')
  execute procedure update_orders_from_payments_storeCredit_fn();


create trigger update_orders_view_from_payments_ccc
    after update or insert on credit_card_charges
    for each row
    execute procedure update_orders_view_from_payments_fn();

create trigger update_orders_view_from_payments_gca
    after update or insert on gift_card_adjustments
    for each row
    execute procedure update_orders_view_from_payments_fn();

create trigger update_orders_view_from_payments_sca
    after update or insert on store_credit_adjustments
    for each row
    execute procedure update_orders_view_from_payments_fn();


