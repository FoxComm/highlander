-- designed for updating products_catalog_vew by scheduled job
-- when activity period is affected

create or replace function toggle_products_catalog_from_to_active() returns boolean as $$
declare
  insert_ids int[];
begin

-- delete outdated products (active -> inactive transition by time)
delete from products_catalog_view where id IN (select p.id
  from products as p
  inner join products_catalog_view as pv on (pv.id = p.id)
  inner join object_forms as f on (f.id = p.form_id)
  inner join object_shadows as s on (s.id = p.shadow_id)
  where
    ((f.attributes->>(s.attributes->'activeFrom'->>'ref')) = '') IS NOT FALSE
    or
    (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp >= CURRENT_TIMESTAMP
    or
      (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS FALSE and
      ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp < CURRENT_TIMESTAMP)));

  -- add new products (inactive -> active transition)
    select array_agg(p.id) into insert_ids
    from products as p
      left join products_catalog_view as pv on (pv.id = p.id)
      inner join object_forms as f on (f.id = p.form_id)
      inner join object_shadows as s on (s.id = p.shadow_id)
    where pv.id is null and -- check that product not in products_catalog_view yet
        (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < CURRENT_TIMESTAMP and
        (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') IS NOT FALSE or
        ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= CURRENT_TIMESTAMP));

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
          inner join product_sku_links_view as sv on (sv.product_id = p.id)
          inner join sku_search_view as sku on (sku.context_id = context.id and sku.code = sv.skus->>0)
        where p.id = ANY(insert_ids);
      end if;

return true;

end;
$$ LANGUAGE plpgsql;
