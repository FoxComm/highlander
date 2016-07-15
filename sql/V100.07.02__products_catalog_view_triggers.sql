--- object contexts
create or replace function refresh_products_cat_from_context_fn() returns trigger as $$
begin
  update products_catalog_view set
    context = subquery.context_name
    from (select
      p.id,
      context.name as context_name
      from products as p
        inner join object_contexts as context on (context.id = p.context_id)
      where context.id = new.id) as subquery
    where subquery.id = products_catalog_view.id;
  return null;
end;
$$ language plpgsql;

create trigger refresh_products_cat_search_view_from_object_contexts
  after update on object_contexts
  for each row
  when (old.name is distinct from new.name)
  execute procedure refresh_products_cat_from_context_fn();

create or replace function refresh_products_cat_search_view_fn() returns trigger as $$
declare
  product_ids int[];
  insert_ids int[];
  update_ids int[];
begin

  case tg_table_name
    when 'products' then
      product_ids := array_agg(new.id);
    when 'product_sku_links_view' then
      product_ids := array_agg(new.product_id);
    when 'product_album_links_view' then
      product_ids := array_agg(new.product_id);
    when 'sku_search_view' then
      select array_agg(p.id) into strict product_ids
        from products as p
          inner join object_contexts as context on (p.context_id = context.id)
          inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
          inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
        where sku.id = new.id;
  end case;

 select array_agg(p.id) into update_ids
    from products as p
      inner join object_contexts as context on (p.context_id = context.id)
      inner join products_catalog_view as pv on (pv.id = p.id and context.name = pv.context)
    where p.id = any(product_ids);

  select array_agg(elements) into insert_ids
    from (
      select unnest(product_ids)
        except
      select unnest(update_ids)
    ) t (elements);

  if array_length(insert_ids, 1) > 0 then
    insert into products_catalog_view select
      p.id,
      f.id as product_id,
      context.name as context,
      f.attributes->>(s.attributes->'title'->>'ref') as title,
      f.attributes->>(s.attributes->'description'->>'ref') as description,
      sku.price as sale_price,
      sku.currency as currency,
      f.attributes->>(s.attributes->'tags'->>'ref') as tags,
      albumLink.albums as albums,
      to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
      from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
        left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
      where p.id = any(insert_ids) and
            (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < current_timestamp and
            (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') is not false or
            ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= current_timestamp));
    end if;

  if array_length(update_ids, 1) > 0 then
    update products_catalog_view set
      product_id = subquery.product_id,
      context = subquery.context,
      title = subquery.title,
      description = subquery.description,
      sale_price = subquery.sale_price,
      currency = subquery.currency,
      tags = subquery.tags,
      albums = subquery.albums,
      archived_at = subquery.archived_at
    from (select
            p.id,
            f.id as product_id,
            context.name as context,
            f.attributes->>(s.attributes->'title'->>'ref') as title,
            f.attributes->>(s.attributes->'description'->>'ref') as description,
            sku.price as sale_price,
            sku.currency as currency,
            f.attributes->>(s.attributes->'tags'->>'ref') as tags,
            albumLink.albums as albums,
            to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
      from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
        left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
      where p.id = any(update_ids) and
            (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < current_timestamp and
            (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') is not false or
            ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= current_timestamp)))
      as subquery
  where products_catalog_view.id = subquery.id;
  end if;

  return null;
end;
$$ language plpgsql;

create trigger insert_products_cat_search_view_from_products
    after insert or update on products
    for each row
    execute procedure refresh_products_cat_search_view_fn();

create trigger refresh_products_cat_search_view_from_p_skus_view
  after insert or update on product_sku_links_view
  for each row
  execute procedure refresh_products_cat_search_view_fn();

create trigger refresh_products_cat_search_view_from_p_albums_view
  after insert or update on product_album_links_view
  for each row
  execute procedure refresh_products_cat_search_view_fn();

create trigger refresh_products_cat_search_view_from_skus_view
  after insert or update on sku_search_view
  for each row
  execute procedure refresh_products_cat_search_view_fn();

--- evict non actual rows
create or replace function delete_inactive_products_cat_search_view_fn() returns trigger as $$
begin

  delete from products_catalog_view where id IN (
    select p.id
    from products as p
        inner join products_catalog_view as pv on (pv.id = p.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
    where p.id = new.id and
      (((f.attributes->>(s.attributes->'activeFrom'->>'ref')) = '') is not false
      or
      (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp >= current_timestamp
      or
        (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS FALSE and
        ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp < current_timestamp))));

return null;
end;
$$ language plpgsql;

create trigger evict_products_cat_search_view_from_object_forms
  after update on products
  for each row
  when (old.shadow_id is distinct from new.shadow_id)
  execute procedure delete_inactive_products_cat_search_view_fn();
