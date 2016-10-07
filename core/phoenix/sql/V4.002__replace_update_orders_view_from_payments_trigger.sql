create or replace function update_orders_view_from_payments_fn() returns trigger as $$
begin
  update orders_search_view set
    payments = subquery.payments
      from (select
      o.id,
      case when count(op) = 0
      then
          '[]'
      else
          json_agg((
              op.payment_method_type,
              op.amount,
              op.currency,
              ccp.state,
              gcc.state,
              sca.state)::export_payments)::jsonb
      end as payments
      from orders as o
      left join order_payments as op on (o.reference_number = op.cord_ref)
      left join credit_card_charges as ccp on (op.id = ccp.order_payment_id)
      left join gift_card_adjustments as gcc on (op.id = gcc.order_payment_id)
      left join store_credit_adjustments as sca on (op.id = sca.order_payment_id)
      where o.id = new.id
      group by o.id) as subquery
    where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_orders_view_from_payments on order_payments;
drop trigger if exists update_orders_view_from_payments_creditCards on order_payments;
drop trigger if exists update_orders_view_from_payments_giftCards on order_payments;
drop trigger if exists update_orders_view_from_payments_storeCredits on order_payments;
drop trigger if exists update_orders_view_from_payments_ccc on credit_card_charges;
drop trigger if exists update_orders_view_from_payments_gca on gift_card_adjustments;
drop trigger if exists update_orders_view_from_payments_sca on store_credit_adjustments;

create trigger update_orders_view_for_payments
    after insert on orders
    for each row
    execute procedure update_orders_view_from_payments_fn();
