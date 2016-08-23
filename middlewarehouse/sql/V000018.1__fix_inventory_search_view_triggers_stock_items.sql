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
        )::jsonb
    where stock_item ->> 'id' = new.id::varchar;
    return null;
end;
$$ language plpgsql;
