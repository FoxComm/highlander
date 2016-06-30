create or replace function update_orders_view_from_customers_ranking_fn() returns trigger as $$
declare
  order_refs text[];
  customer_ids int[];
begin

   case TG_TABLE_NAME
     when 'orders' then
       order_refs := array_agg(NEW.reference_number::text);
     when 'order_payments' then
       order_refs := array_agg(NEW.order_ref);
     when 'credit_card_charges' then
       select array_agg(op.order_ref) into strict order_refs
         from credit_card_charges as ccp
         inner join order_payments as op on (op.id = ccp.order_payment_id)
         WHERE ccp.id = NEW.id;
     when 'gift_card_adjustments' then
       select array_agg(op.order_ref) into strict order_refs
         from gift_card_adjustments as gca
         inner join order_payments as op on (op.id = gca.order_payment_id)
         WHERE gca.id = NEW.id;
     when 'store_credit_adjustments' then
       select array_agg(op.order_ref) into strict order_refs
         from store_credit_adjustments as sca
         inner join order_payments as op on (op.id = sca.order_payment_id)
         where sca.id = NEW.id;
     when 'returns' then
       order_refs := array_agg(NEW.order_ref);
     when 'return_payments' then
       select array_agg(returns.order_ref) into strict order_refs
       from return_payments as rp
       inner join returns on (rp.return_id = returns.id)
       where rp.id = NEW.id;
   end case;

  select array_agg(c.id) into strict customer_ids
    from orders
    inner join customers as c on (orders.customer_id = c.id)
    where orders.reference_number = ANY(order_refs);


  update orders_search_view set
    customer =
       -- TODO: uncomment when jsonb_set in 9.5 will be available
       -- jsonb_set(customer, '{revenue}', jsonb (subquery.revenue::varchar), true)
       json_build_object(
               'id', customer ->> 'id',
               'name', customer ->> 'name',
               'email', customer ->> 'email',
               'is_blacklisted', customer ->> 'is_blacklisted',
               'joined_at', customer ->> 'joined_at',
               'rank', customer ->> 'rank',
               'revenue', subquery.revenue
           )::jsonb
      from (
            select
                c.id,
                coalesce(sum(CCc.amount),0) + coalesce(sum(SCa.debit), 0) + coalesce(sum(GCa.debit),0) - coalesce(sum(rp.amount),0) as revenue
            from customers as c
              inner join orders on(c.id = orders.customer_id and orders.state in ('remorseHold', 'fulfillmentStarted', 'shipped'))
              inner join order_payments as op on(op.order_ref = orders.reference_number)
              left join credit_card_charges as CCc on(CCc.order_payment_id = op.id and CCc.state in ('auth', 'fullCapture'))
              left join store_credit_adjustments as SCa on(SCA.order_payment_id = op.id and SCa.state in ('auth', 'capture'))
              left join gift_card_adjustments as GCa on (GCa.order_payment_id = op.id and GCa.state in ('auth', 'capture'))
              left join returns as r on (r.order_ref = orders.reference_number and r.state = 'complete')
              left join return_payments as rp on (rp.return_id = r.id and rp.amount is not null)
            where is_guest = false and c.id = ANY(customer_ids)
              group by (c.id)
              order by revenue desc)
            AS subquery
    where orders_search_view.customer ->> 'id' = subquery.id::varchar;

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_customers_ranking_on_orders
    after update or insert on orders
    for each row
    when (new.state in ('remorseHold', 'fulfillmentStarted', 'shipped'))
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_order_payments
    after update or insert on order_payments
    for each row
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_ccc
    after update or insert on credit_card_charges
    for each row
    when (new.state in ('auth', 'fullCapture'))
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_gca
    after update or insert on gift_card_adjustments
    for each row
    when (new.state in ('auth', 'capture'))
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_sca
    after update or insert on store_credit_adjustments
    for each row
    when (new.state in ('auth', 'capture'))
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_returns
    after update or insert on returns
    for each row
    when (new.state = 'complete')
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_returns_payments
    after update or insert on return_payments
    for each row
    when (new.amount is not null)
    execute procedure update_orders_view_from_customers_ranking_fn();
