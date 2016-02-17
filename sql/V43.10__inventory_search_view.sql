create materialized view inventory_search_view as
select distinct on (sku.code)
  sku.id,
  sku.name as product,
  -- TODO: replace with product.is_active
  sku.is_active as product_active,
  sku.code,
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
order by sku.code;

create unique index inventory_search_view_idx on inventory_search_view (code);
