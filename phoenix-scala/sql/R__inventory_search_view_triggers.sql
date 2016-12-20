create or replace function insert_skus_view_from_skus_fn() returns trigger as $$
begin
  insert into sku_search_view select
    new.id as id,
    new.code as sku_code,
    context.name as context,
    context.id as context_id,
    illuminate_text(sku_form, sku_shadow, 'title') as title,
    illuminate_obj(sku_form, sku_shadow, 'images')->>0 as image,
    illuminate_obj(sku_form, sku_shadow, 'salePrice')->>'value' as sale_price,
    illuminate_obj(sku_form, sku_shadow, 'salePrice')->>'currency' as sale_price_currency,
    to_char(new.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
    illuminate_obj(sku_form, sku_shadow, 'retailPrice')->>'value' as retail_price,
    illuminate_obj(sku_form, sku_shadow, 'retailPrice')->>'currency' as retail_price_currency,
    illuminate_obj(sku_form, sku_shadow, 'externalId') as external_id,
    new.scope as scope
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
    sale_price = subquery.sale_price,
    sale_price_currency = subquery.sale_price_currency,
    archived_at = subquery.archived_at,
    retail_price = subquery.retail_price,
    retail_price_currency = subquery.retail_price_currency,
    external_id = subquery.external_id
    from (select
        variant.id,
        variant.code,
        illuminate_text(form, shadow, 'title') as title,
        illuminate_obj(form, shadow, 'salePrice')->>'value' as sale_price,
        illuminate_obj(form, shadow, 'salePrice')->>'currency' as sale_price_currency,
        to_json_timestamp(variant.archived_at) as archived_at,
        illuminate_obj(form, shadow, 'retailPrice')->>'value' as retail_price,
        illuminate_obj(form, shadow, 'retailPrice')->>'currency' as retail_price_currency,
        form.attributes->(shadow.attributes->'externalId'->>'ref') as external_id
      from product_variants as variant
      inner join object_forms as form on (form.id = variant.form_id)
      inner join object_shadows as shadow on (shadow.id = sku.shadow_id)
      where variant.id = new.id) as subquery
      where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_skus_view_from_object_context_fn() returns trigger as $$
begin
  update sku_search_view set
    context = subquery.name,
    context_id = subquery.id,
    archived_at = subquery.archived_at
    from (select
        o.id,
        o.name,
        variants.id as variant_id,
        to_json_timestamp(variants.archived_at) as archived_at
      from object_contexts as o
      inner join product_variants as variants on (variants.context_id = o.id)
      where variants.id = new.id) as subquery
      where subquery.variant_id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_skus_view_image_fn() returns trigger as $$
declare
    variant_ids int[];
begin

  case tg_table_name
    when 'product__variant_links' then
    select array_agg(product__variant_links.right_id) into variant_ids
        from product__variant_links
        where product__variant_links.id = new.id;
    when 'product_album_links_view' then
      select array_agg(variant.id) into variant_ids
      from product_variants as variant
        inner join product__variant_links on variant.id = product__variant_links.right_id
        where product__variant_links.left_id = new.product_id;
  end case;

  update sku_search_view
    set image = subquery.image
  from (select variant.id as id,
               variant.code as code,
               (product_album_links_view.albums #>> '{0, images, 0, src}') as image
        from product_variants as variant
          inner join product__variant_links on variant.id = product__variant_links.right_id
          inner join product_album_links_view on product__variant_links.left_id = product_album_links_view.product_id
        where variant.id = any(variant_ids)) as subquery
  where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;