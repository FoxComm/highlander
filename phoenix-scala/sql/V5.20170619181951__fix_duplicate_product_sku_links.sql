-- Something introduced duplicate SKU-product links, so this must delete older duplicate rows
delete from product_sku_links where id in (
  select max(id) from product_sku_links
  where archived_at is null
  group by (left_id, right_id) having count(1) > 1
);

-- Make sure this bug never occurs again: links must be unique
-- Omitting `constraint` keyword makes postgres generate constraint name
alter table product_sku_links add unique (left_id, right_id, archived_at);
