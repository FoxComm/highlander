create or replace function update_inventory_transactions_view_from_stock_locations_fn() returns trigger as $$
begin
    update inventory_transactions_search_view set
        stock_location_name = new.name;
    return null;
end;
$$ language plpgsql;

create trigger update_inventory_transactions_view_from_stock_locations
    after update on stock_locations
    for each row
    execute procedure update_inventory_transactions_view_from_stock_locations_fn();
