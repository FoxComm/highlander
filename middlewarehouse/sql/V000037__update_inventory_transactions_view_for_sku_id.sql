CREATE OR REPLACE FUNCTION public.update_inventory_transactions_view_from_transactions_insert_fn()
    RETURNS TRIGGER
AS
$BODY$
DECLARE
    skuCode            sku_code;
    stockItemId        INTEGER;
    stockLocationId    INTEGER;
    stockLocationName  generic_string;
    stockLocationScope exts.ltree;
BEGIN
    SELECT
        si.id,
        s.code,
        sl.id,
        sl.name,
        sl.scope
    INTO stockItemId, skuCode, stockLocationId, stockLocationName, stockLocationScope
    FROM stock_items si
        INNER JOIN stock_locations sl ON sl.id = si.stock_location_id
        INNER JOIN skus s ON s.id = si.sku_id
    WHERE si.id = new.stock_item_id;

    INSERT INTO inventory_transactions_search_view SELECT DISTINCT ON (new.id)
                                                       new.id                                   AS id,
                                                       skuCode                                  AS sku,
                                                       stockLocationName                        AS stock_location_name,
                                                       new.type                                 AS type,
                                                       new.status                               AS status,
                                                       new.quantity_new -
                                                       new.quantity_change                      AS quantity_previous,
                                                       new.quantity_new                         AS quantity_new,
                                                       new.quantity_change                      AS quantity_change,
                                                       new.afs_new                              AS afs_new,
                                                       to_char(new.created_at,
                                                               'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS created_at,
                                                       stockItemId                              AS stock_item_id,
                                                       stockLocationId                          AS stock_location_id,
                                                       stockLocationScope                       AS scope;
    RETURN NULL;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;