create or replace function update_inventory_view_from_stock_items_fn() returns trigger as $$
declare
    skuCode sku_code;
begin
    update inventory_search_view set
        sku = new.sku,
        stock_item = json_build_object(
            'id', new.id,
            'sku', new.sku,
            'defaultUnitCost', new.default_unit_cost
        )::jsonb;
    return null;
end;
$$ language plpgsql;

create trigger update_inventory_view_from_stock_items
    after update on stock_items
    for each row
    execute procedure update_inventory_view_from_stock_items_fn();
