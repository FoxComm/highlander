create or replace function update_customers_ranking() returns boolean as $$
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

	return true;
end;
$$ language plpgsql;

create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
    begin
        insert into orders_search_view select distinct on (new.id)
            -- order
            new.id as id,
            new.reference_number as reference_number,
            new.state as state,
            to_char(new.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as placed_at,
            new.currency as currency,
            -- totals
            new.sub_total as sub_total,
            new.shipping_total as shipping_total,
            new.adjustments_total as adjustments_total,
            new.taxes_total as taxes_total,
            new.grand_total as grand_total,
            -- customer
            json_build_object(
                'id', c.id,
                'name', c.name,
                'email', c.email,
                'is_blacklisted', c.is_blacklisted,
                'joined_at', c.joined_at,
                'rank', c.rank,
                'revenue', c.revenue
            )::jsonb as customer
            from customers_search_view as c
            where (new.customer_id = c.id);
        return null;
    end;
$$ language plpgsql;

create or replace function update_carts_view_from_carts_insert_fn() returns trigger as $$
    begin
        insert into carts_search_view select distinct on (new.id)
            -- order
            new.id as id,
            new.reference_number as reference_number,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
            to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
            new.currency as currency,
            -- totals
            new.sub_total as sub_total,
            new.shipping_total as shipping_total,
            new.adjustments_total as adjustments_total,
            new.taxes_total as taxes_total,
            new.grand_total as grand_total,
            -- customer
            json_build_object(
                'id', c.id,
                'name', c.name,
                'email', c.email,
                'is_blacklisted', c.is_blacklisted,
                'joined_at', c.joined_at,
                'rank', c.rank,
                'revenue', c.revenue
            )::jsonb as customer
            from customers_search_view as c
            where (new.customer_id = c.id);
        return null;
    end;
$$ language plpgsql;