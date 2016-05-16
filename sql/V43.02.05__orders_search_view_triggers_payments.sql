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
      select array_agg(order_id) into strict order_ids
        from store_credit_adjustments as sca
        inner join order_payments as op on (op.id = sca.order_payment_id)
        WHERE sca.id = NEW.id;
  end case;

  update orders_search_view_test set
    (payments,credit_card_count,credit_card_total) = (select
      case when count(op) = 0
      then
          '[]'
      else
          json_agg((op.payment_method_type, op.amount, op.currency, ccp.state, gcc.state, sca.state)::export_payments)
      end as payments,
      count(opc.id) as credit_card_count,
      coalesce(sum(opc.amount), 0) as credit_card_total
      from orders as o
      left join order_payments as op on (o.id = op.order_id)
      left join order_payments as opc on (o.id = opc.order_id and opc.payment_method_type = 'creditCard')
      left join credit_card_charges as ccp on (op.id = ccp.order_payment_id)
      left join gift_card_adjustments as gcc on (op.id = gcc.order_payment_id)
      left join store_credit_adjustments as sca on (op.id = sca.order_payment_id)
      where o.id = ANY(order_ids));

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_line_items
    after update or insert on order_payments
    for each row
    execute procedure update_orders_view_from_payments_fn();

create trigger update_orders_view_from_line_items
    after update or insert on credit_card_charges
    for each row
    execute procedure update_orders_view_from_payments_fn();

create trigger update_orders_view_from_line_items
    after update or insert on gift_card_adjustments
    for each row
    execute procedure update_orders_view_from_payments_fn();

create trigger update_orders_view_from_line_items
    after update or insert on store_credit_adjustments
    for each row
    execute procedure update_orders_view_from_payments_fn();
