--- Procedures to update customer rakings

drop function update_customers_ranking(); -- changing return type, cannot be replaced
create function update_customers_ranking() returns trigger as $$
begin
	 -- Update customers ranks
      update customers_search_view
        set rank = q.rank from (
            select
              c.id,
              c.revenue,
              ntile(100) over (w) as rank
            from
              customers_search_view as c
            where revenue > 0
              window w as (order by c.revenue desc)
              order by revenue desc) as q
          where customers_search_view.id = q.id;

      -- Update Carts
      update carts_search_view set
        customer = q.customer from (
          select
              cs.id,
              c.rank,
              jsonb_set(jsonb_set(cs.customer, '{rank}', jsonb (c.rank::varchar), true),
                        '{revenue}', jsonb (c.revenue::varchar), true)
                        as customer
          from carts_search_view as cs
          inner join customers_search_view as c on (c.id = (cs.customer->>'id')::bigint)
          where c.rank > 0
        ) as q
        where carts_search_view.id = q.id;

      -- Update Orders
      update orders_search_view set
        customer = q.customer from (
          select
              o.id,
              c.rank,
              jsonb_set(jsonb_set(o.customer, '{rank}', jsonb (c.rank::varchar), true),
                        '{revenue}', jsonb (c.revenue::varchar), true)
                      as customer
          from orders_search_view as o
          inner join customers_search_view as c on (c.id = (o.customer->>'id')::bigint)
          where c.rank > 0
        ) as q
        where orders_search_view.id = q.id;

	return null;
end;
$$ language plpgsql;

drop trigger if exists update_ccustomers_ranking_from_orders_trigger on orders;
create trigger update_ccustomers_ranking_from_orders_trigger
    after insert or update on orders
    for each row
    execute procedure update_customers_ranking();
