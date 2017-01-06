CREATE OR REPLACE FUNCTION public.update_inventory_view_from_summary_insert_fn()
    RETURNS TRIGGER
AS
$BODY$
DECLARE
    skuCode            sku_code;
    stockItem          JSONB;
    stockLocation      JSONB;
    stockLocationScope exts.ltree;
BEGIN
    skuCode := (
        SELECT s.code
        FROM stock_items si
        JOIN skus s ON si.sku_id = s.id
        WHERE si.id = new.stock_item_id
    );

    stockItem := (
        SELECT json_build_object(
            'id', query.id,
            'sku', query.code,
            'defaultUnitCost', query.default_unit_cost
        ) :: JSONB
        FROM (
          SELECT stock_items.id, skus.code, stock_items.default_unit_cost
          FROM stock_items
          JOIN skus ON skus.id = stock_items.sku_id
        ) AS query
        WHERE new.stock_item_id = query.id
    );

    SELECT
        json_build_object(
            'id', sl.id,
            'name', sl.name,
            'type', sl.type
        ) :: JSONB AS stock_location,
        sl.scope
    INTO stockLocation, stockLocationScope
    FROM stock_items AS si
        INNER JOIN stock_locations sl ON sl.id = si.stock_location_id
    WHERE new.stock_item_id = si.id;

    INSERT INTO inventory_search_view SELECT DISTINCT ON (new.id)
                                          -- summary
                                          new.id                                                   AS id,
                                          skuCode                                                  AS sku,
                                          -- stock_item object
                                          stockItem,
                                          -- stock_locatoin object
                                          stockLocation,
                                          new.type                                                 AS type,

                                          new.on_hand                                              AS on_hand,
                                          new.on_hold                                              AS on_hold,
                                          new.reserved                                             AS reserved,
                                          new.shipped                                              AS shipped,
                                          new.afs                                                  AS afs,
                                          new.afs_cost                                             AS afs_cost,
                                          to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS created_at,
                                          to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS updated_at,
                                          to_char(new.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS deleted_at,
                                          stockLocationScope                                       AS scope;
    RETURN NULL;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;

