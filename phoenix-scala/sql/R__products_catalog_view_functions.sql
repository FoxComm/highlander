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
          inner join sku_search_view as sku on (sku.context_id = context.id and sku.sku_code = sv.skus->>0)
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
      sku.sale_price as sale_price,
      sku.sale_price_currency as currency,
      f.attributes->>(s.attributes->'tags'->>'ref') as tags,
      albumLink.albums as albums
      from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.sku_code = sv.skus->>0)
        left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
      where p.id = any(insert_ids) and
            (p.archived_at is null or (p.archived_at)::timestamp > current_timestamp) and
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
            sku.sale_price as sale_price,
            sku.sale_price_currency as currency,
            f.attributes->>(s.attributes->'tags'->>'ref') as tags,
            albumLink.albums as albums,
            case ((p.archived_at is null or (p.archived_at)::timestamp > current_timestamp) and
                (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < current_timestamp and
                (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') is not false or
                ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= current_timestamp)))
            when true
              then null
            else
              to_json_timestamp(now()::timestamp)
            end as archived_at
      from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.sku_code = sv.skus->>0)
        left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
      where p.id = any(update_ids))
      as subquery
  where products_catalog_view.id = subquery.id;
  end if;

  return null;
end;
$$ language plpgsql;
