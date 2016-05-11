create or replace function update_orders_view_from_customers_fn() returns trigger as $$
declare
    affected_orders record;
begin

  UPDATE orders_search_view_test SET
    customer = json_build_object(
        'id', new.id,
        'name', new.name,
        'email', new.email,
        'is_blacklisted', new.is_blacklisted,
        'joined_at', to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        'rank', customer ->> 'rank',
        'revenue', customer ->> 'revenue'
    ) WHERE customer ->> 'id' = new.id::VARCHAR;

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_customers
    after update on customers
    for each row
    execute procedure update_orders_view_from_customers_fn();
