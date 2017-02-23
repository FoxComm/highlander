create or replace function insert_product_variants_view_from_product_variants_fn() returns trigger as $$
begin
  insert into product_variants_search_view select
    new.id as id,
    mwh_sku.sku_code as sku_code,
    context.name as context,
    context.id as context_id,
    illuminate_text(form, shadow, 'title') as title,
    illuminate_obj(form, shadow, 'images')->>0 as image,
    illuminate_obj(form, shadow, 'salePrice')->>'value' as sale_price,
    illuminate_obj(form, shadow, 'salePrice')->>'currency' as sale_price_currency,
    to_json_timestamp(new.archived_at) as archived_at,
    illuminate_obj(form, shadow, 'retailPrice')->>'value' as retail_price,
    illuminate_obj(form, shadow, 'retailPrice')->>'currency' as retail_price_currency,
    illuminate_obj(form, shadow, 'externalId') as external_id,
    new.scope as scope,
    new.form_id as variant_id,
    mwh_sku.sku_id as sku_id,
    to_json_timestamp(new.created_at) as created_at
    from object_contexts as context
      inner join object_shadows as shadow  on (shadow.id = new.shadow_id)
      inner join object_forms as form on (form.id = new.form_id)
      inner join product_variant_skus as mwh_sku on (mwh_sku.variant_form_id = new.form_id)
    where context.id = new.context_id;

  return null;
end;
$$ language plpgsql;

drop trigger if exists insert_product_variants_view_from_product_variants on product_variants;
create trigger insert_product_variants_view_from_product_variants
  after insert on product_variants
  for each row
  execute procedure insert_product_variants_view_from_product_variants_fn();

create or replace function update_product_variants_view_from_product_variant_links_fn() returns trigger as $$
begin
  update product_variants_search_view set
    product_id = subquery.product_id
    from (select
            product.form_id as product_id,
            variant.form_id as variant_id
          from product_variants as variant
            inner join products as product on (product.id = new.left_id)
          where variant.id = new.right_id) as subquery
    where subquery.variant_id = product_variants_search_view.variant_id;

  return null;
end;
$$ language plpgsql; 

drop trigger if exists update_product_variants_view_from_product_variant_links on product_to_variant_links;
create trigger update_product_variants_view_from_product_variant_links
  after insert or update on product_to_variant_links
  for each row
  execute procedure update_product_variants_view_from_product_variant_links_fn();

create or replace function update_product_variants_view_from_object_attrs_fn() returns trigger as $$
begin
  update product_variants_search_view set
    sku_code = subquery.code,
    title = subquery.title,
    sale_price = subquery.sale_price,
    sale_price_currency = subquery.sale_price_currency,
    archived_at = subquery.archived_at,
    retail_price = subquery.retail_price,
    retail_price_currency = subquery.retail_price_currency,
    external_id = subquery.external_id
    from (select
        variant.form_id,
        variant.code,
        illuminate_text(form, shadow, 'title') as title,
        illuminate_obj(form, shadow, 'salePrice')->>'value' as sale_price,
        illuminate_obj(form, shadow, 'salePrice')->>'currency' as sale_price_currency,
        to_json_timestamp(variant.archived_at) as archived_at,
        to_json_timestamp(variant.created_at) as created_at,
        illuminate_obj(form, shadow, 'retailPrice')->>'value' as retail_price,
        illuminate_obj(form, shadow, 'retailPrice')->>'currency' as retail_price_currency,
        form.attributes->(shadow.attributes->'externalId'->>'ref') as external_id
      from product_variants as variant
        inner join object_forms as form on (form.id = variant.form_id)
        inner join object_shadows as shadow on (shadow.id = variant.shadow_id)
      where variant.id = new.id) as subquery
      where subquery.form_id = product_variants_search_view.variant_id;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_product_variants_view_from_object_head_and_shadows on product_variants;
create trigger update_product_variants_view_from_object_head_and_shadows
  after update on product_variants
  for each row
  when (old.form_id is distinct from new.form_id or
        old.shadow_id is distinct from new.shadow_id or
        old.code is distinct from new.code or
        old.archived_at is distinct from new.archived_at)
  execute procedure update_product_variants_view_from_object_attrs_fn();

create or replace function update_product_variants_view_from_object_context_fn() returns trigger as $$
begin
  update product_variants_search_view set
    context = subquery.name,
    context_id = subquery.id,
    archived_at = subquery.archived_at,
    created_at = subquery.created_at
    from (select
        o.id,
        o.name,
        variants.form_id as product_variant_id,
        to_json_timestamp(variants.archived_at) as archived_at,
        to_json_timestamp(variants.created_at) as created_at
      from object_contexts as o
      inner join product_variants as variants on (variants.context_id = o.id)
      where variants.id = new.id) as subquery
      where subquery.product_variant_id = product_variants_search_view.variant_id;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_product_variants_view_from_object_forms on object_contexts;
create trigger update_product_variants_view_from_object_forms
  after update or insert on object_contexts
  for each row
  execute procedure update_product_variants_view_from_object_context_fn();

create or replace function update_product_variants_view_image_fn() returns trigger as $$
declare
    product_variant_ids int[];
begin

  case tg_table_name
    when 'product_to_variant_links' then
    select array_agg(product_to_variant_links.right_id) into product_variant_ids
        from product_to_variant_links
        where product_to_variant_links.id = new.id;
    when 'product_album_links_view' then
      select array_agg(variant.id) into product_variant_ids
      from product_variants as variant
        inner join product_to_variant_links on variant.id = product_to_variant_links.right_id
        where product_to_variant_links.left_id = new.product_id;
  end case;

  update product_variants_search_view
    set image = subquery.image
  from (select variant.id as id,
               variant.code as code,
               (product_album_links_view.albums #>> '{0, images, 0, src}') as image
        from product_variants as variant
          inner join product_to_variant_links on variant.id = product_to_variant_links.right_id
          inner join product_album_links_view on product_to_variant_links.left_id = product_album_links_view.product_id
        where variant.id = any(product_variant_ids)) as subquery
  where subquery.id = product_variants_search_view.id;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_product_variants_view_image on product_album_links_view;
create trigger update_product_variants_view_image
  after insert or update on product_album_links_view
  for each row
  execute procedure update_product_variants_view_image_fn();

drop trigger if exists update_product_variants_view_image on product_to_variant_links;
create trigger update_product_variants_view_image
  after insert or update on product_to_variant_links
  for each row
  execute procedure update_product_variants_view_image_fn();

create or replace function update_variants_album_view_image_fn() returns trigger as $$
begin
  update product_variants_search_view
    set image = subquery.image
  from (select album_view.product_variant_id as id,
               (album_view.albums #>> '{0, images, 0, src}') as image
        from variant_album_links_view as album_view
        where album_view.product_variant_id = new.product_variant_id) as subquery
  where subquery.id = product_variants_search_view.id;

  return null;
end;
$$ language plpgsql;

drop trigger if exists update_variants_album_view_image on variant_album_links_view;
create trigger update_variants_album_view_image
  after insert or update on variant_album_links_view
  for each row
  execute procedure update_variants_album_view_image_fn();
