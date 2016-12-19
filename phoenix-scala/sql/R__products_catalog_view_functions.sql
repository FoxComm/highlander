create or replace function refresh_products_cat_search_view_fn() returns trigger as $$
declare
  product_ids int[];
  insert_ids int[];
  update_ids int[];
  delete_ids int[];
begin

  case tg_table_name
    when 'products' then
      product_ids := array_agg(new.id);
    when 'product__variant_links_view' then
      product_ids := array_agg(new.product_id);
    when 'product_album_links_view' then
      product_ids := array_agg(new.product_id);
    when 'inventory_search_view' then
      select array_agg(p.id) into strict product_ids
        from products as p
          inner join object_contexts as context on (p.context_id = context.id)
          inner join product__variant_links_view as pv on (pv.product_id = p.id) --get list of sku codes for the product
          inner join inventory_search_view as inv on (inv.context_id = context.id and inv.sku_code = pv.skus->>0)
        where inv.id = new.id;
  end case;

  with temp_table(id, alive, catalog_id) as (
    select
         q.id,
         q.alive,
         q.catalog_id
       from (select
               p.id,
               ((p.archived_at is null or (p.archived_at) :: timestamp > statement_timestamp()) and
               ((f.attributes ->> (s.attributes -> 'activeFrom' ->> 'ref')) = '') is false and
                (f.attributes ->> (s.attributes -> 'activeFrom' ->> 'ref')) :: timestamp <
                statement_timestamp() and
                (((f.attributes ->> (s.attributes -> 'activeTo' ->> 'ref')) = '') is not false or
                 ((f.attributes ->> (s.attributes -> 'activeTo' ->> 'ref')) :: timestamp >=
                  statement_timestamp())))
                 as alive,
                pv.id as catalog_id

             from products as p
               inner join object_contexts as context on (p.context_id = context.id)
               left join products_catalog_view as pv on (pv.id = p.id and context.name = pv.context)
               inner join object_forms as f on (f.id = p.form_id)
               inner join object_shadows as s on (s.id = p.shadow_id)
                where p.id = any(product_ids)) as q
) select
    array_agg(id) filter (where alive = true and catalog_id is not null) as upd_ids,
    array_agg(id) filter (where alive = false and catalog_id is not null) as del_ids,
    array_agg(id) filter (where catalog_id is null and alive = true) as ins_ids
      into update_ids, delete_ids, insert_ids
  from temp_table;

  if array_length(delete_ids, 1) > 0 then
    delete from products_catalog_view where id = any(delete_ids);
  end if;

  if array_length(insert_ids, 1) > 0 then
    insert into products_catalog_view select
      p.id,
      f.id as product_id,
      context.name as context,
      f.attributes->>(s.attributes->'title'->>'ref') as title,
      f.attributes->>(s.attributes->'description'->>'ref') as description,
      inv.sale_price as sale_price,
      inv.sale_price_currency as currency,
      f.attributes->>(s.attributes->'tags'->>'ref') as tags,
      albumLink.albums as albums,
      p.scope as scope,
      pv.skus as skus
      from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product__variant_links_view as pv on (pv.product_id = p.id) --get list of sku codes for the product
        inner join inventory_search_view as inv on (inv.context_id = context.id and inv.sku_code = pv.skus->>0)
        left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
      where p.id = any(insert_ids);
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
        scope = subquery.scope,
        skus = subquery.skus
      from (select
                p.id,
                f.id as product_id,
                context.name as context,
                f.attributes->>(s.attributes->'title'->>'ref') as title,
                f.attributes->>(s.attributes->'description'->>'ref') as description,
                inv.sale_price as sale_price,
                inv.sale_price_currency as currency,
                f.attributes->>(s.attributes->'tags'->>'ref') as tags,
                albumLink.albums as albums,
                ((p.archived_at is null or (p.archived_at)::timestamp > statement_timestamp()) and
                    ((f.attributes ->> (s.attributes -> 'activeFrom' ->> 'ref')) = '') is false and
                    (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < statement_timestamp() and
                    (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') is not false or
                    ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= statement_timestamp())))
                as alive,
                p.scope as scope,
                pv.skus as skus
              from products as p
                inner join object_contexts as context on (p.context_id = context.id)
                inner join object_forms as f on (f.id = p.form_id)
                inner join object_shadows as s on (s.id = p.shadow_id)
                -- get list of sku codes for the product
                inner join product__variant_links_view as pv on (pv.product_id = p.id)
                inner join inventory_search_view as inv on (inv.context_id = context.id and inv.sku_code = pv.skus->>0)
                left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
              where p.id = any(update_ids)
           ) as subquery
    where products_catalog_view.id = subquery.id;
  end if;

  return null;
end;
$$ language plpgsql;
