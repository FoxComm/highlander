ALTER TABLE addresses ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE carriers ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE inventory_search_view ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE inventory_transactions_search_view ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE shipment_line_items ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE shipment_transactions ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE shipments ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE shipping_methods ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE stock_item_summaries ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE stock_item_transactions ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE stock_item_units ADD COLUMN scope exts.ltree NOT NULL;
ALTER TABLE stock_items ADD COLUMN scope exts.ltree NOT NULL;

DELETE FROM stock_locations;
ALTER TABLE stock_locations ADD COLUMN scope exts.ltree NOT NULL;


CREATE OR REPLACE FUNCTION public.update_inventory_transactions_view_from_transactions_insert_fn()
    RETURNS TRIGGER
AS
$BODY$
DECLARE
    skuCode           sku_code;
    stockItemId       INTEGER;
    stockLocationId   INTEGER;
    stockLocationName generic_string;
BEGIN
    SELECT
        si.id,
        si.sku,
        sl.id,
        sl.name
    INTO stockItemId, skuCode, stockLocationId, stockLocationName
    FROM stock_items si
        LEFT JOIN stock_locations sl ON sl.id = si.stock_location_id
    WHERE si.id = new.stock_item_id AND si.scope = new.scope;

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
                                                       new.scope                                AS scope;
    RETURN NULL;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;


CREATE OR REPLACE FUNCTION public.update_inventory_view_from_summary_insert_fn()
    RETURNS TRIGGER
AS
$BODY$
DECLARE
    skuCode        sku_code;
    stock_item     JSONB;
    stock_location JSONB;
BEGIN
    skuCode := (SELECT si.sku
                FROM stock_items si
                WHERE si.id = new.stock_item_id);

    stock_item := (
        SELECT json_build_object(
            'id', si.id,
            'sku', si.sku,
            'defaultUnitCost', si.default_unit_cost
        ) :: JSONB
        FROM stock_items AS si
        WHERE (new.stock_item_id = si.id)
    );

    stock_location := (
        SELECT json_build_object(
                   'id', sl.id,
                   'name', sl.name,
                   'type', sl.type
               ) :: JSONB AS stock_location
        FROM stock_items AS si
            LEFT JOIN stock_locations sl ON sl.id = si.stock_location_id
        WHERE (new.stock_item_id = si.id)
    );

    INSERT INTO inventory_search_view SELECT DISTINCT ON (new.id)
                                          -- summary
                                          new.id                                                   AS id,
                                          skuCode                                                  AS sku,
                                          -- stock_item object
                                          stock_item,
                                          -- stock_locatoin object
                                          stock_location,
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
                                          new.scope                                                AS scope;
    RETURN NULL;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;

