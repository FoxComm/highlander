create materialized view inventory_search_view as
select row_number() over (order by (code, sku_type)) as id, code, product, on_hand, on_hold, reserved, safety_stock, afs, sku_type from
  (select distinct on (sku.code)
    product.attributes->'default'->>'title' as product,
    -- TODO: replace with product.is_active
    sku.is_active as product_active,
    sku.code,
    sku.is_active as sku_active,
    warehouse.name as warehouse,
    summary.id as summary_id
  from
    skus as sku
    inner join inventory_summaries as summary on (summary.sku_id = sku.id)
    inner join warehouses as warehouse on (summary.warehouse_id = warehouse.id)
    inner join products as product on (sku.product_id = product.id)
  ) as details
join
  (select
    summary.id as summary_id,
    on_hand,
    on_hold,
    reserved,
    safety_stock,
    on_hand - on_hold - reserved - safety_stock as afs,
    'sellable' as sku_type
  from
    sellable_inventory_summaries as sel inner join inventory_summaries as summary on sel.id = summary.sellable_id
  union select
    summary.id as summary_id,
    on_hand,
    on_hold,
    reserved,
    null as safety_stock,
    on_hand - on_hold - reserved as afs,
    'preorder' as sku_type
  from
    preorder_inventory_summaries as pre inner join inventory_summaries as summary on pre.id = summary.preorder_id
  union select
    summary.id as summary_id,
    on_hand,
    on_hold,
    reserved,
    null as safety_stock,
    on_hand - on_hold - reserved as afs,
    'backorder' as sku_type
  from
    backorder_inventory_summaries as bac inner join inventory_summaries as summary on bac.id = summary.backorder_id
  union select
    summary.id as summary_id,
    on_hand,
    on_hold,
    reserved,
    null as safety_stock,
    on_hand - on_hold - reserved as afs,
    'nonsellable' as sku_type
  from
    nonsellable_inventory_summaries as non inner join inventory_summaries as summary on non.id = summary.nonsellable_id
  ) as summaries
on details.summary_id = summaries.summary_id
order by (code, sku_type);

create unique index inventory_search_view_idx on inventory_search_view (product, sku_type);
