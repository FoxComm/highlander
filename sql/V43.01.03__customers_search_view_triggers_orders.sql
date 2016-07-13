create or replace function update_customers_view_from_orders_fn() returns trigger as $$
begin
    update customers_search_view set
        order_count = subquery.order_count,
        orders = subquery.orders
        from (select
                c.id,
                count(o.id) as order_count,
                case when count(o) = 0
                  then
                    '[]'
                else
                  json_agg((
                    o.customer_id,
                    o.reference_number,
                    o.state,
                    to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                    o.sub_total,
                    o.shipping_total,
                    o.adjustments_total,
                    o.taxes_total,
                    o.grand_total,
                    0
                  )::export_orders)::jsonb
                end as orders
              from customers as c
              left join orders as o on (o.customer_id = c.id)
              where c.id = new.customer_id
              group by c.id) AS subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create trigger update_customers_view_from_orders
    after update on orders
    for each row
    execute procedure update_customers_view_from_orders_fn();
