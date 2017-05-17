create or replace function update_inventory_transactions_view_from_transactions_insert_fn() returns trigger as $$
declare
    skuId integer;
    skuCode sku_code;
    stockItemId integer;
    stockLocationId integer;
    stockLocationName generic_string;
    stockLocationScope exts.ltree;
begin
    select
        si.id,
        sku.id,
        si.sku,
        sl.id,
        sl.name,
        sl.scope
    into stockItemId, skuId, skuCode, stockLocationId, stockLocationName, stockLocationScope
    from stock_items si
        inner join stock_locations sl on sl.id = si.stock_location_id
        inner join skus sku on sku.code = si.sku
    where si.id = new.stock_item_id;

    insert into inventory_transactions_search_view select distinct on (new.id)
        new.id as id,
        skuCode as sku,
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
        stockLocationScope as scope,
        skuId as sku_id;
    return null;
end;
$$ language plpgsql;

-- update on stock_items update
create or replace function update_inventory_transactions_view_from_stock_items_fn() returns trigger as $$
begin
    update inventory_transactions_search_view set
        sku = new.sku
    where stock_location_id = new.id;
    return null;
end;
$$ language plpgsql;

-- update on stock_locations update
create or replace function update_inventory_transactions_view_from_stock_locations_fn() returns trigger as $$
begin
    update inventory_transactions_search_view set
        stock_location_name = new.name
    where stock_location_id = new.id;
    return null;
end;
$$ language plpgsql;
