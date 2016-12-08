create or replace function update_inventory_transactions_view_from_transactions_insert_fn() returns trigger as $$
declare
    skuId integer;
    skuCode sku_code;
    stockItemId integer;
    stockLocationId integer;
    stockLocationName generic_string;
begin
    select si.id, si.sku_id, si.sku_code, sl.id, sl.name
    into stockItemId, skuId, skuCode, stockLocationId, stockLocationName
    from stock_items si
    left join stock_locations sl ON sl.id = si.stock_location_id
    where si.id = new.stock_item_id;

    insert into inventory_transactions_search_view select distinct on (new.id)
        new.id as id,
        skuCode as sku_code,
        stockLocationName as stock_location_name,
        new.type as type,
        new.status as status,
        new.quantity_new - new.quantity_change as quantity_previous,
        new.quantity_new as quantity_new,
        new.quantity_change as quantity_change,
        new.afs_new as afs_new,
        to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
        stockItemId as stock_item_id,
        stockLocationId as stock_location_id,
        skuId as sku_id;
    return null;
end;
$$ language plpgsql;

-- update on stock_items update
create or replace function update_inventory_transactions_view_from_stock_items_fn() returns trigger as $$
begin
    update inventory_transactions_search_view set
        sku_id = new.sku_id,
        sku_code = new.sku_code
    where stock_location_id = new.id;
    return null;
end;
$$ language plpgsql;
