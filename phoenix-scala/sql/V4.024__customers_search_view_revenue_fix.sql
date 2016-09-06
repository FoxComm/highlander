drop materialized view customers_ranking;
alter table customers_search_view add column revenue bigint not null default 0;
alter table customers_search_view add column rank bigint;

create or replace function update_customers_view_revenue_fn() returns trigger as $$
begin
    update customers_search_view set 
        revenue = subquery.revenue 
        from (
			select
				c.id as customer_id,
		    	coalesce(sum(ccc.amount), 0) + coalesce(sum(sca.debit), 0) + coalesce(sum(gca.debit), 0) as revenue
		    from customers as c
		    inner join orders on (c.id = orders.customer_id and orders.state = 'shipped')
		    inner join order_payments as op on (op.cord_ref = orders.reference_number)
		    left join credit_card_charges as ccc on (ccc.order_payment_id = op.id and ccc.state = 'fullCapture')
		    left join store_credit_adjustments as sca on (sca.order_payment_id = op.id and sca.state = 'capture')
		    left join gift_card_adjustments as gca on (gca.order_payment_id = op.id and gca.state = 'capture')
		    where is_guest = false and c.id = new.customer_id
		    group by c.id
		    order by c.id) as subquery
    where customers_search_view.id = subquery.customer_id;
    return null;
end;
$$ language plpgsql;

create trigger update_customers_view_revenue
    after insert or update on orders
    for each row
    when (new.state = 'shipped')
    execute procedure update_customers_view_revenue_fn();
