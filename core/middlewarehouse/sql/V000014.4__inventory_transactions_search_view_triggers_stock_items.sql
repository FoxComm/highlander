create or replace function update_inventory_transactions_view_from_stock_items_fn() returns trigger as $$
begin
    update inventory_transactions_search_view set
        sku = new.sku;
    return null;
end;
$$ language plpgsql;

create trigger update_inventory_transactions_view_from_stock_items
    after update on stock_items
    for each row
    execute procedure update_inventory_transactions_view_from_stock_items_fn();
