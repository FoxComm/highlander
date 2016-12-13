create or replace function update_inventory_view_from_summary_insert_fn() returns trigger as $$
declare
    skuId integer;
    skuCode sku_code;
    stockItem jsonb;
    stockLocation jsonb;
    stockLocationScope exts.ltree;
begin
    select si.sku_id, si.sku_code
    into skuId, skuCode
    from stock_items si
    where si.id = new.stock_item_id;

    stockItem := (
        select json_build_object(
            'id', si.id,
            'skuId', si.sku_id,
            'skuCode', si.sku_code,
            'defaultUnitCost', si.default_unit_cost
        )::jsonb
        from stock_items as si
        where (new.stock_item_id = si.id)
    );

    select
        json_build_object(
            'id', sl.id,
            'name', sl.name,
            'type', sl.type
        ) :: JSONB AS stock_location,
        sl.scope
    into stockLocation, stockLocationScope
    from stock_items AS si
        inner join stock_locations sl on sl.id = si.stock_location_id
    where new.stock_item_id = si.id;

    insert into inventory_search_view select distinct on (new.id)
        -- summary
        new.id as id,
        skuCode,
        -- stock_item object
        stockItem,
        -- stock_locatoin object
        stockLocation,
        new.type as type,

        new.on_hand as on_hand,
        new.on_hold as on_hold,
        new.reserved as reserved,
        new.shipped as shipped,
        new.afs as afs,
        new.afs_cost as afs_cost,
        to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
        to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
        to_char(new.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as deleted_at,
        stockLocationScope                                       AS scope,
        skuId as sku_id;
    return null;
end;
$$ language plpgsql;

create or replace function update_inventory_view_from_stock_items_fn() returns trigger as $$
declare
    skuCode sku_code;
begin
    update inventory_search_view set
        sku_id = new.sku_id,
        sku_code = new.sku_code,
        stock_item = json_build_object(
            'id', new.id,
            'skuId', new.sku_id,
            'skuCode', new.sku_code,
            'defaultUnitCost', new.default_unit_cost
        )::jsonb
    where stock_item ->> 'id' = new.id::varchar;
    return null;
end;
$$ language plpgsql;

