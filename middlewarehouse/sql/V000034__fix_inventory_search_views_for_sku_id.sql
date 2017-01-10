create or replace function update_inventory_view_from_stock_items_fn() returns trigger as $$
declare
    skuCode sku_code;
begin
    update inventory_search_view set
        sku = skus.code,
        stock_item = json_build_object(
            'id', new.id,
            'sku', skus.code,
            'defaultUnitCost', new.default_unit_cost
        )::jsonb
    from skus
    where stock_item ->> 'id' = new.id::varchar and new.sku_id = skus.id;
    return null;
end;
$$ language plpgsql;

create or replace function update_inventory_transactions_view_from_stock_items_fn() returns trigger as $$
begin
    update inventory_transactions_search_view set
        sku = skus.code
    from skus
    where stock_item_id = new.id and skus.id = new.sku_id;
    return null;
end;
$$ language plpgsql;