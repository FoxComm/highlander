create or replace function refresh_products_cat_search_view_fn() returns trigger as $$
declare
  product_ids int[];
begin

  case TG_TABLE_NAME
    when 'products' then
      product_ids := array_agg(NEW.id);
    when 'object_contexts' then
      select array_agg(p.id) into strict product_ids
        from object_contexts as context
        inner join products as p on (p.context_id = context.id)
        where context.id = NEW.id;
    when 'object_forms' then
      select array_agg(p.id) into strict product_ids
        from object_forms as f
        inner join products as p on (f.id = p.form_id)
        where f.id = NEW.id;
    when 'object_shadows' then
      select array_agg(p.id) into strict product_ids
        from object_shadows as s
        inner join products as p on (s.id = p.shadow_id)
        where s.id = NEW.id;
    when 'product_sku_links_view' then
      product_ids := array_agg(NEW.product_id);
    when 'sku_search_view' then
      select array_agg(p.id) into strict product_ids
        from products as p
          inner join object_contexts as context on (p.context_id = context.id)
          inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
          inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
        where sku.id = NEW.id;
  end case;


  insert into products_catalog_view select
      p.id,
      f.id as product_id,
      context.name as context,
      f.attributes->>(s.attributes->'title'->>'ref') as title,
      f.attributes->(s.attributes->'images'->>'ref') as images,
      f.attributes->>(s.attributes->'description'->>'ref') as description,
      sku.price as sale_price,
      sku.currency as currency,
      f.attributes->>(s.attributes->'tags'->>'ref') as tags
      from products as p
        left join products_catalog_view as pv on (pv.id = p.id)
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
      where p.id = ANY(product_ids) and
            (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < CURRENT_TIMESTAMP and
            (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS NOT FALSE or
            ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= CURRENT_TIMESTAMP))
      on conflict (id, context) do
        update set
          product_id = excluded.product_id,
          context = excluded.context,
          title = excluded.title,
          images = excluded.images,
          description = excluded.description,
          sale_price = excluded.sale_price,
          currency = excluded.currency,
          tags = excluded.tags
      where products_catalog_view.id = excluded.id;

  return null;
end;
$$ language plpgsql;


create trigger insert_products_cat_search_view_from_products
    after insert on products
    for each row
    execute procedure refresh_products_cat_search_view_fn();

create trigger refresh_products_cat_search_view_from_object_contexts
  after insert or update on object_contexts
  for each row
  execute procedure refresh_products_cat_search_view_fn();

create trigger refresh_products_cat_search_view_from_object_forms
  after insert or update on object_forms
  for each row
  when (NEW.kind = 'product')
  execute procedure refresh_products_cat_search_view_fn();

create trigger refresh_products_cat_search_view_from_object_shadows
  after insert or update on object_shadows
  for each row
  execute procedure refresh_products_cat_search_view_fn();


create trigger refresh_products_cat_search_view_from_p_skus_view
  after insert or update on product_sku_links_view
  for each row
  execute procedure refresh_products_cat_search_view_fn();

create trigger refresh_products_cat_search_view_from_skus_view
  after insert or update on sku_search_view
  for each row
  execute procedure refresh_products_cat_search_view_fn();


--- evict non actual rows
create or replace function delete_inactive_products_cat_search_view_fn() returns trigger as $$
declare
  affected_ids int[];
  current_ids int[];
  del_ids int[];
begin
  case TG_TABLE_NAME
    when 'object_forms' then
      select array_agg(p.id) into affected_ids
        from object_forms as f
        inner join products as p on (f.id = p.form_id)
        where f.id = NEW.id;
    when 'object_shadows' then
      select array_agg(p.id) into affected_ids
        from object_shadows as s
        inner join products as p on (s.id = p.shadow_id)
        where s.id = NEW.id;
    when 'product_sku_links_view' then
      affected_ids := array_agg(NEW.product_id);
    when 'sku_search_view' then
      select array_agg(p.id) into affected_ids
        from products as p
          inner join object_contexts as context on (p.context_id = context.id)
          inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
          inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
        where sku.id = NEW.id;
  end case;

  select array_agg(p.id) into current_ids
    from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        -- get list of sku codes for the product
        inner join product_sku_links_view as sv on (sv.product_id = p.id)
        -- get first sku code and make sure we are in same context
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
    where p.id = ANY(affected_ids) and
          (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < CURRENT_TIMESTAMP and
          (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS NOT FALSE or
          ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= CURRENT_TIMESTAMP));

  if array_length(affected_ids, 1) <> array_length(current_ids, 1) and array_length(affected_ids, 1) > 0
   then
      select array_agg(elements) into del_ids
        from (
          select unnest(affected_ids)
            except
          select unnest(current_ids)
        ) t (elements);

      if array_length(del_ids, 1) > 0 then
        raise notice 'delete from catalog by trigger: %', del_ids;
        delete from products_catalog_view where id = ANY(del_ids);
      end if;
  end if;

return null;
end;
$$ language plpgsql;

create trigger evict_products_cat_search_view_from_object_forms
  after update on object_forms
  for each row
  when (NEW.kind = 'product')
  execute procedure delete_inactive_products_cat_search_view_fn();

create trigger evict_products_cat_search_view_from_object_shadows
  after update on object_shadows
  for each row
  execute procedure delete_inactive_products_cat_search_view_fn();

create trigger evict_products_cat_search_view_from_p_skus_view
  after update on product_sku_links_view
  for each row
  when (OLD.skus is distinct from NEW.skus)
  execute procedure delete_inactive_products_cat_search_view_fn();

create trigger evict_products_cat_search_view_from_skus_view
  after update on sku_search_view
  for each row
  when (OLD.context_id is distinct from NEW.context_id or OLD.code is distinct from NEW.code)
  execute procedure delete_inactive_products_cat_search_view_fn();

