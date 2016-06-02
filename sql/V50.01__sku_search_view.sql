create table sku_search_view
(
    id integer not null,
    code generic_string not null,
    context generic_string not null,
    context_id integer not null,
    title text,
    image text,
    price text,
    currency text
);
create unique index sku_search_view_idx on sku_search_view (id, context);

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
    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'currency' as currency
    from object_contexts as context
       left join  object_shadows as sku_shadow on (sku_shadow.id = NEW.shadow_id)
       left join object_forms as sku_form on (sku_form.id = NEW.form_id)
    where context.id = NEW.context_id;


  return null;
end;
$$ language plpgsql;

create trigger insert_skus_view_from_skus
    after insert on skus
    for each row
    execute procedure insert_skus_view_from_skus_fn();

create or replace function update_skus_view_from_skus_fn() returns trigger as $$
begin

  update sku_search_view set
    code = NEW.code
    where id = NEW.id;

  return null;
end;
$$ language plpgsql;


create trigger update_skus_view_from_skus
    after update on skus
    for each row
    execute procedure update_skus_view_from_skus_fn();

create or replace function update_skus_view_from_object_context_fn() returns trigger as $$
begin

  update sku_search_view set
    context = subquery.name,
    context_id = subquery.id
    from (select
        o.id,
        o.name,
        skus.id as sku_id
      from object_contexts as o
      inner join skus on (skus.form_id = o.id)
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
declare sku_ids int[];
begin
  case TG_TABLE_NAME
    when 'object_shadows' then
      select array_agg(skus.id) into strict sku_ids
        from skus
        inner join object_shadows as sku_shadow on (skus.shadow_id = sku_shadow.id)
        where sku_shadow.id = NEW.id;
    when 'object_forms' then
     select array_agg(skus.id) into strict sku_ids
       from skus
       inner join object_forms as sku_form on (skus.form_id = sku_form.id)
       where sku_form.id = NEW.id;
  end case;

  update sku_search_view set
    title = subquery.title,
    image = subquery.image,
    price = subquery.price,
    currency = subquery.currency
    from (select
        sku.id,
        sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref') as title,
        sku_form.attributes->(sku_shadow.attributes->'images'->>'ref')->>0 as image,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as price,
        sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'currency' as currency
      from skus as sku
      inner join object_forms as sku_form on (sku_form.id = sku.form_id)
      inner join object_shadows as sku_shadow on (sku_shadow.id = sku.shadow_id)
      where sku.id = ANY(sku_ids)) as subquery
      where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;


create trigger update_skus_view_from_object_shadows
    after update or insert on object_shadows
    for each row
    execute procedure update_skus_view_from_object_attrs_fn();

create trigger update_skus_view_from_object_forms
  after update or insert on object_forms
  for each row
  execute procedure update_skus_view_from_object_attrs_fn();
