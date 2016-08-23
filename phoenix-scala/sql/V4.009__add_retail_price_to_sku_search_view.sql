----- update view
alter table sku_search_view add column retail_price text;
alter table sku_search_view add column retail_price_currency text;
alter table sku_search_view rename column price to sale_price;
alter table sku_search_view rename column currency to sale_price_currency;
alter table sku_search_view rename column code to sku_code;

----- update trigger functions
create or replace function insert_skus_view_from_skus_fn() returns trigger as $$
begin
  insert into sku_search_view select
    new.id as id,
    new.code as sku_code,
    context.name as context,
    context.id as context_id,
    sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref') as title,
    sku_form.attributes->(sku_shadow.attributes->'images'->>'ref')->>0 as image,
    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as sale_price,
    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'currency' as sale_price_currency,
    to_char(new.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
    sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'value' as retail_price,
    sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'currency' as retail_price_currency
    from object_contexts as context
       inner join object_shadows as sku_shadow on (sku_shadow.id = new.shadow_id)
       inner join object_forms as sku_form on (sku_form.id = new.form_id)
    where context.id = new.context_id;

  return null;
end;
$$ language plpgsql;

create or replace function update_skus_view_from_object_attrs_fn() returns trigger as $$
begin
  update sku_search_view set
    sku_code = subquery.code,
    title = subquery.title,
    image = subquery.image,
    sale_price = subquery.sale_price,
    sale_price_currency = subquery.sale_price_currency,
    archived_at = subquery.archived_at,
    retail_price = subquery.retail_price,
    retail_price_currency = subquery.retail_price_currency
    from (select
        sku.id,
        sku.code,
        sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref') as title,
        sku_form.attributes->(sku_shadow.attributes->'images'->>'ref')->>0 as image,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as sale_price,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'currency' as sale_price_currency,
        to_char(sku.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
        sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'value' as retail_price,
        sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'currency' as retail_price_currency
      from skus as sku
      inner join object_forms as sku_form on (sku_form.id = sku.form_id)
      inner join object_shadows as sku_shadow on (sku_shadow.id = sku.shadow_id)
      where sku.id = new.id) as subquery
      where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

--- include archived_at updates in sku trigger
drop trigger if exists update_skus_view_from_object_shadows on skus;
create trigger update_skus_view_from_object_head_and_shadows
    after update on skus
    for each row
    when (old.form_id is distinct from new.form_id or
          old.shadow_id is distinct from new.shadow_id or
          old.code is distinct from new.code or
          old.archived_at is distinct from new.archived_at)
    execute procedure update_skus_view_from_object_attrs_fn();

--- products
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
      albums = subquery.albums
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
            to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
      from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.sku_code = sv.skus->>0)
        left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
      where p.id = any(update_ids) and
            (p.archived_at is null or (p.archived_at)::timestamp > current_timestamp) and
            (f.attributes->>(s.attributes->'activeFrom'->>'ref'))::timestamp < current_timestamp and
            (((f.attributes->>(s.attributes->'activeTo'->>'ref')) = '') is not false or
            ((f.attributes->>(s.attributes->'activeTo'->>'ref'))::timestamp >= current_timestamp)))
      as subquery
  where products_catalog_view.id = subquery.id;
  end if;

  return null;
end;
$$ language plpgsql;
