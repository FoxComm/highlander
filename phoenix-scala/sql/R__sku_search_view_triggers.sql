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
    sku_form.attributes->(sku_shadow.attributes->'retailPrice'->>'ref')->>'currency' as retail_price_currency,
    sku_form.attributes->(sku_shadow.attributes->'externalId'->>'ref') as external_id,
    new.scope as scope,
    new.form_id as sku_id
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
    sku_id = subquery.form_id,
    title = subquery.title,
    sale_price = subquery.sale_price,
    sale_price_currency = subquery.sale_price_currency,
    archived_at = subquery.archived_at,
    retail_price = subquery.retail_price,
    retail_price_currency = subquery.retail_price_currency,
    external_id = subquery.external_id,
    scope = subquery.scope
    from (select
        sku.id,
        sku.code,
        sku.form_id,
        sku.scope as scope,
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
