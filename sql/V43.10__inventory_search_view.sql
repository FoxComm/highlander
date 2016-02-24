create materialized view inventory_search_view as
select distinct on (sku.sku)
  sku.id,
  product.attributes->'default'->>'title',
  -- TODO: replace with product.is_active
  product.is_active as product_active,
  sku.sku,
  sku.is_active as sku_active,
  sku.type as sku_type,
  warehouse.name as warehouse,
  inventory.on_hand,
  inventory.on_hold,
  inventory.reserved,
  inventory.safety_stock,
  inventory.on_hand - inventory.reserved - inventory.on_hold - inventory.safety_stock as afs
from skus as sku
inner join inventory_summaries as inventory on (inventory.sku_id = sku.id)
inner join warehouses as warehouse on (inventory.warehouse_id = warehouse.id)
inner join products as product on (sku.product_id = product.id)
order by sku.sku;

create unique index inventory_search_view_idx on inventory_search_view (sku);
