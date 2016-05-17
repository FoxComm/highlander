create or replace function update_orders_view_from_orders_fn() returns trigger as $$
    begin
        insert into orders_search_view select distinct on (new.id)
            -- order
            new.id as id,
            new.reference_number as reference_number,
            new.state as state,
            to_char(new.created_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as created_at,
            to_char(new.placed_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as placed_at,
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
                'joined_at', to_char(c.created_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"'),
                'rank', rank.rank,
                'revenue', coalesce(rank.revenue, 0)
            ) as customer
            from customers as c
            left join customers_ranking as rank on (c.id = rank.id)
            where (new.customer_id = c.id)
          -- update only order stuff
on conflict(id) do update set
    id = excluded.id,
    reference_number = excluded.reference_number,
    state = excluded.state,
    created_at = excluded.created_at,
    placed_at = excluded.placed_at,
    currency = excluded.currency;

      return null;
  end;
$$ language plpgsql;


create trigger update_orders_view_from_orders
    after update or insert on orders
    for each row
    execute procedure update_orders_view_from_orders_fn();