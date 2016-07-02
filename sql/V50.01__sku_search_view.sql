create table sku_search_view
(
    id integer not null,
    code generic_string not null,
    context generic_string not null,
    context_id integer not null,
    title text,
    image text,
    price text,
    currency text,
    archived_at generic_string
);
create unique index sku_search_view_idx on sku_search_view (id, lower(context));


create or replace function insert_skus_view_from_skus_fn() returns trigger as $$
begin

  insert into sku_search_view select
    NEW.id as id,
    NEW.code as code,
    context.name as context,
    context.id as context_id,
    sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref') as title,
    sku_form.attributes->(sku_shadow.attributes->'images'->>'ref')->>0 as image,
    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as price,
    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'currency' as currency,
    to_char(NEW.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
    from object_contexts as context
       inner join object_shadows as sku_shadow on (sku_shadow.id = NEW.shadow_id)
       inner join object_forms as sku_form on (sku_form.id = NEW.form_id)
    where context.id = NEW.context_id;


  return null;
end;
$$ language plpgsql;

create trigger insert_skus_view_from_skus
    after insert on skus
    for each row
    execute procedure insert_skus_view_from_skus_fn();

create or replace function update_skus_view_from_object_context_fn() returns trigger as $$
begin

  update sku_search_view set
    context = subquery.name,
    context_id = subquery.id,
    archived_at = subquery.archived_at
    from (select
        o.id,
        o.name,
        skus.id as sku_id,
        to_char(skus.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
      from object_contexts as o
      inner join skus on (skus.context_id = o.id)
      where skus.id = NEW.id) as subquery
      where subquery.sku_id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;


create trigger update_skus_view_from_object_forms
    after update or insert on object_contexts
    for each row
    execute procedure update_skus_view_from_object_context_fn();


create or replace function update_skus_view_from_object_attrs_fn() returns trigger as $$
begin

  update sku_search_view set
    code = subquery.code,
    title = subquery.title,
    image = subquery.image,
    price = subquery.price,
    currency = subquery.currency,
    archived_at = subquery.archived_at
    from (select
        sku.id,
        sku.code,
        sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref') as title,
        sku_form.attributes->(sku_shadow.attributes->'images'->>'ref')->>0 as image,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as price,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'currency' as currency,
        to_char(sku.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
      from skus as sku
      inner join object_forms as sku_form on (sku_form.id = sku.form_id)
      inner join object_shadows as sku_shadow on (sku_shadow.id = sku.shadow_id)
      where sku.id = NEW.id) as subquery
      where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;


create trigger update_skus_view_from_object_shadows
    after update on skus
    for each row
    when (OLD.form_id is distinct from NEW.form_id or
          OLD.shadow_id is distinct from NEW.shadow_id or
          OLD.code is distinct from NEW.code)
    execute procedure update_skus_view_from_object_attrs_fn();
