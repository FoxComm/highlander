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
        sku.id,
        sku.code,
        sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref') as title,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as sale_price,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'currency' as sale_price_currency,
        to_char(sku.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
        sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'value' as retail_price,
        sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'currency' as retail_price_currency,
        sku_form.attributes->(sku_shadow.attributes->'externalId'->>'ref') as external_id
      from skus as sku
      inner join object_forms as sku_form on (sku_form.id = sku.form_id)
      inner join object_shadows as sku_shadow on (sku_shadow.id = sku.shadow_id)
      where sku.id = new.id) as subquery
      where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_skus_view_image_fn() returns trigger as $$
declare sku_ids int[];
begin

  case tg_table_name
    when 'product_sku_links' then
    select array_agg(product_sku_links.right_id) into sku_ids
      from product_sku_links
      where product_sku_links.id = new.id;
    when 'product_album_links_view' then
      select array_agg(ids.id) into sku_ids from (
        select sku.id
        from skus as sku
          inner join product_sku_links on sku.id = product_sku_links.right_id
        where product_sku_links.left_id = new.product_id
        union
        select sku.id
        from skus as sku
           inner join variant_value_sku_links as vsku_link on (vsku_link.right_id = sku.id)
           inner join variant_variant_value_links as vvlink on (vsku_link.left_id = vvlink.right_id)
           inner join product_variant_links as pvlink on (vvlink.left_id = pvlink.right_id) where pvlink.left_id = new.product_id) as ids;
  end case;

  update sku_search_view
  set image = subquery.image
  from (select sku.id as id,
               sku.code as code,
               (product_album_links_view.albums #>> '{0, images, 0, src}') as image
        from skus as sku
          inner join product_sku_links on sku.id = product_sku_links.right_id
          inner join product_album_links_view on product_sku_links.left_id = product_album_links_view.product_id
        where sku.id = any(sku_ids)
        union
        select sku.id as id,
               sku.code as code,
               (product_album_links_view.albums #>> '{0, images, 0, src}') as image
         from skus as sku
           inner join variant_value_sku_links as vsku_link on (vsku_link.right_id = sku.id)
           inner join variant_variant_value_links as vvlink on (vsku_link.left_id = vvlink.right_id)
           inner join product_variant_links as pvlink on (vvlink.left_id = pvlink.right_id)
           inner join product_album_links_view on pvlink.left_id = product_album_links_view.product_id
         where sku.id = any(sku_ids)) as subquery
  where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

