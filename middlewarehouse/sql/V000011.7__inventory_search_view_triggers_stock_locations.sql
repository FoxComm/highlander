create or replace function update_inventory_view_from_stock_locations_fn() returns trigger as $$
begin
    update inventory_search_view set
        stock_location = json_build_object(
            'id', new.id,
            'name', new.name,
            'type', new.type
        )::jsonb;
    return null;
end;
$$ language plpgsql;

create trigger update_inventory_view_from_stock_locations
    after update on stock_locations
    for each row
    execute procedure update_inventory_view_from_stock_locations_fn();
