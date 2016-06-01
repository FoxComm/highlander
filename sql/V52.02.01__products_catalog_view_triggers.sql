create or replace function refresh_products_catalog_search_view_fn() returns trigger as $$
declare
  product_ids int[];
  insert_ids int[];
  update_ids int[];
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

  select array_agg(elements) into strict insert_ids
    from (
      select unnest(product_ids)
        except
      select id from products_catalog_view
    ) t (elements);

  select array_agg(elements) into strict update_ids
    from (
      select unnest(product_ids)
        except
      select unnest(insert_ids)
    ) t (elements);

  if array_length(insert_ids, 1) > 0 then
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
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
      where p.id = ANY(insert_ids) and
            (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < CURRENT_TIMESTAMP and
            (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS NOT FALSE or
            ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= CURRENT_TIMESTAMP));
    end if;

  if array_length(update_ids, 1) > 0 then
    update products_catalog_view set
      product_id = subquery.product_id,
      context = subquery.context,
      title = subquery.title,
      images = subquery.images,
      description = subquery.description,
      sale_price = subquery.sale_price,
      currency = subquery.currency,
      tags = subquery.tags
    from (select
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
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
      where p.id = ANY(update_ids) and
            (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < CURRENT_TIMESTAMP and
            (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS NOT FALSE or
            ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= CURRENT_TIMESTAMP)))
      as subquery
  where products_catalog_view.id = subquery.id;
  end if;


  return null;
end;
$$ language plpgsql;


create trigger insert_products_catalog_search_view_from_products
    after insert on products
    for each row
    execute procedure refresh_products_catalog_search_view_fn();

create trigger refresh_products_catalog_search_view_from_object_contexts
  after insert or update on object_contexts
  for each row
  execute procedure refresh_products_catalog_search_view_fn();

create trigger refresh_products_catalog_search_view_from_object_forms
  after insert or update on object_forms
  for each row
  execute procedure refresh_products_catalog_search_view_fn();

create trigger refresh_products_catalog_search_view_from_object_shadows
  after insert or update on object_shadows
  for each row
  execute procedure refresh_products_catalog_search_view_fn();


create trigger refresh_products_catalog_search_view_from_product_sku_links_view
  after insert or update on product_sku_links_view
  for each row
  execute procedure refresh_products_catalog_search_view_fn();

create trigger refresh_products_catalog_search_view_from_sku_search_view
  after insert or update on sku_search_view
  for each row
  execute procedure refresh_products_catalog_search_view_fn();



