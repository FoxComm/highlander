create or replace function update_orders_view_from_customers_ranking_fn() returns trigger as $$
declare order_ids int[];
begin
  case TG_TABLE_NAME
    when 'orders' then
      order_ids := array_agg(NEW.id);
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
    when 'rmas' THEN
      order_ids := array_agg(NEW.order_id);
    when 'rma_payments' then
      select array_agg(rmas.order_id) into strict order_ids
      from rma_payments as rp
      inner join rmas on (rp.rma_id = rmas.id)
      where rp.id = NEW.id;
  end case;

  -- TODO: update to || jsonb feature when 9.5 will be available

  update orders_search_view set
    customer =
      json_build_object(
        'id', customer ->> 'id',
        'name', customer ->> 'name',
        'email', customer ->> 'email',
        'is_blacklisted', customer ->> 'is_blacklisted',
        'joined_at', customer ->> 'joined_at',
        'rank', subquery.rank,
        'revenue', coalesce(subquery.revenue, 0)
    )::jsonb
      from (select
              ranking.id,
              ranking.revenue,
              ntile(100) over (w) as rank
            from
              (select
                  c.id,
                  coalesce(sum(CCc.amount),0) + coalesce(sum(SCa.debit), 0) + coalesce(sum(GCa.debit),0) - coalesce(sum(rp.amount),0) as revenue
                from customers as c
                inner join orders on(c.id = orders.customer_id and orders.state in ('remorseHold', 'fulfillmentStarted', 'shipped'))
                inner join order_payments as op on(op.order_id = orders.id)
                left join credit_card_charges as CCc on(CCc.order_payment_id = op.id and CCc.state in ('auth', 'fullCapture'))
                left join store_credit_adjustments as SCa on(SCA.order_payment_id = op.id and SCa.state in ('auth', 'capture'))
                left join gift_card_adjustments as GCa on (GCa.order_payment_id = op.id and GCa.state in ('auth', 'capture'))
                left join rmas on(rmas.order_id = orders.id and rmas.state = 'complete')
                left join rma_payments as rp on (rp.rma_id = rmas.id and rp.amount is not null)
                where is_guest = false AND orders.id = ANY(order_ids)
                group by (c.id)
                order by revenue desc
              ) as ranking
            where revenue > 0
            window w as (order by ranking.revenue desc)
            order by revenue desc) AS subquery
    WHERE orders_search_view.customer ->> 'id' = subquery.id::varchar;

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

create trigger update_orders_view_from_customers_ranking_on_credit_card_charges
    after update or insert on credit_card_charges
    for each row
    when (new.state in ('auth', 'fullCapture'))
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_gift_card_adjustments
    after update or insert on gift_card_adjustments
    for each row
    when (new.state in ('auth', 'capture'))
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_store_credit_adjustments
    after update or insert on store_credit_adjustments
    for each row
    when (new.state in ('auth', 'capture'))
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_rmas
    after update or insert on rmas
    for each row
    when (new.state = 'complete')
    execute procedure update_orders_view_from_customers_ranking_fn();

create trigger update_orders_view_from_customers_ranking_on_rma_payments
    after update or insert on rma_payments
    for each row
    when (new.amount is not null)
    execute procedure update_orders_view_from_customers_ranking_fn();
