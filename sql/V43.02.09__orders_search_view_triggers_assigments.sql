create or replace function update_orders_view_from_assignments_fn() returns trigger as $$
declare order_refs text[];
begin
  case TG_TABLE_NAME
    when 'assignments' then
      select array_agg(o.id) into strict order_refs
      from assignments as a
      inner join orders as o on (o.id = a.reference_id)
      where a.id = NEW.id and a.reference_type = 'order';
    when 'store_admins' then
      select array_agg(o.id) into strict order_refs
      from orders as o
      inner join assignments as a on (o.id = a.reference_id and a.reference_type = 'order')
      inner join store_admins as sa on (sa.id = a.store_admin_id)
      where sa.id = NEW.id;
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
                json_agg((sa.name, to_char(a.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_assignees)::jsonb
            end as assignees
        from orders as o
        left join assignments as a on (o.id = a.reference_id and a.reference_type = 'order')
        left join store_admins as sa on (sa.id = a.store_admin_id)
        where o.reference_number = ANY(order_refs)
        group by o.id) AS subquery
  WHERE orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_assignments_fn
    after update or insert on assignments
    for each row
    when (NEW.reference_type = 'order')
    execute procedure update_orders_view_from_assignments_fn();

create trigger update_orders_view_from_assignments_store_admin_fn
    after update or insert on store_admins
    for each row
    execute procedure update_orders_view_from_assignments_fn();
