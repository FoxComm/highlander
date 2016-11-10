alter table sku_search_view add sku_id integer;

update sku_search_view
  set sku_id = q.form_id from (
    select id, form_id from skus) as q
  where sku_search_view.id = q.id;

alter table sku_search_view alter column sku_id set not null;
