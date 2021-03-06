create or replace function update_orders_view_from_assignments_fn() returns trigger as $$
declare cord_refs bigint[];
begin
  case tg_table_name
    when 'orders' then
      select array_agg(o.id) into strict cord_refs
      from orders as o
      where o.id = new.id;
    when 'assignments' then
      select array_agg(o.id) into strict cord_refs
      from assignments as a
      inner join orders as o on (o.id = a.reference_id)
      where a.id = new.id and a.reference_type = 'order';
    when 'store_admins' then
      select array_agg(o.id) into strict cord_refs
      from orders as o
      inner join assignments as a on (o.id = a.reference_id and a.reference_type = 'order')
      inner join store_admins as sa on (sa.id = a.store_admin_id)
      where sa.id = new.id;
  end case;

  update orders_search_view set
    assignment_count = subquery.count,
    assignees = subquery.assignees
    from (select
            o.id,
            count(a.id) as count,
            case when count(sa) = 0
            then
                '[]'
            else
                json_agg((
                    sa.name,
                    to_char(a.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_assignees)::jsonb
            end as assignees
        from orders as o
        left join assignments as a on (o.id = a.reference_id and a.reference_type = 'order')
        left join store_admins as sa on (sa.id = a.store_admin_id)
        where o.id = any(cord_refs)
        group by o.id) as subquery
  where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create trigger update_order_view_for_orders_fn
    after insert on orders
    for each row
    execute procedure update_orders_view_from_assignments_fn();

