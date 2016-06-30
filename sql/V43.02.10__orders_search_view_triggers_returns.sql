create or replace function update_orders_view_from_returns_fn() returns trigger as $$
begin

  update orders_search_view set
    return_count = subquery.count,
    returns = subquery.returns
    from (select
            o.id as order_id,
            count(r.id) as count,
            case when count(r) = 0
            then
                '[]'
            else
                json_agg((r.reference_number, r.state, r.return_type, to_char(r.created_at,
                'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_returns)::jsonb
            end as returns
        from orders as o
        left join returns as r on (o.reference_number = r.order_ref)
        where o.reference_number = NEW.order_ref
        GROUP BY o.id) AS subquery
  WHERE orders_search_view.id = subquery.order_id;

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_returns_fn
    after update or insert on returns
    for each row
    execute procedure update_orders_view_from_returns_fn();
