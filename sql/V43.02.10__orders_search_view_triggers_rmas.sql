create or replace function update_orders_view_from_rmas_fn() returns trigger as $$
begin

  update orders_search_view set
    rma_count = subquery.count,
    rmas = subquery.rmas
    from (select
            o.id as order_id,
            count(rmas.id) as count,
            case when count(rmas) = 0
            then
                '[]'
            else
                json_agg((rmas.reference_number, rmas.state, rmas.rma_type, to_char(rmas.created_at,
                'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_rmas)::jsonb
            end as rmas
        from orders as o
        left join rmas on (o.id = rmas.order_id)
        where o.id = NEW.order_id
        GROUP BY o.id) AS subquery
  WHERE orders_search_view.id = subquery.order_id;

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_rmas_fn
    after update or insert on rmas
    for each row
    execute procedure update_orders_view_from_rmas_fn();
