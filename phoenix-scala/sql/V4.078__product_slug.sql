alter table products
  add column slug generic_string;

create unique index product_slug_idx on products(lower(slug), context_id);

create or replace function generate_slug(title text)
  returns varchar as $$
declare
  similar_slugs text [];
  slug_index    int;
  new_slug      text;
begin

  title := regexp_replace(lower(title), '\s+', ' ', 'g');
  title := replace(title, '&', 'and');
  title := replace(title, ' ', '-');
  title := regexp_replace(title, E'[^\\w -]', '', 'g');

  if (title = '') is not false
  then
    title := 'unnamed-product';
  end if;

  --handle duplication by adding index: e.g. product-slug-2
  select array_agg(cast(slug as text)) into strict similar_slugs
  from products
  where slug ~* (title || '(-[\\d]+)?');

  new_slug := title;
  slug_index := 2;

  while array_length(similar_slugs, 1) > 0 and new_slug = any (similar_slugs)
  loop
    new_slug := title || '-' || CAST(slug_index as text);
    slug_index := slug_index + 1;
  end loop;

  return new_slug;

end;
$$ language plpgsql;

alter table products_catalog_view
  add column slug generic_string;
alter table products_search_view
  add column slug generic_string;

create or replace function insert_products_search_view_from_products_fn() returns trigger as $$
begin

  insert into products_search_view select
                                     new.id as id,
                                     f.id as product_id,
                                     context.name as context,
                                     f.attributes->>(s.attributes->'title'->>'ref') as title,
                                     f.attributes->>(s.attributes->'description'->>'ref') as description,
                                     f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
                                     f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
                                     f.attributes->>(s.attributes->'tags'->>'ref') as tags,
                                     link.skus as skus,
                                     albumLink.albums as albums,
                                     to_char(new.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
                                     f.attributes->>(s.attributes->'externalId'->>'ref') as external_id,
                                     p.scope as scope,
                                     p.slug as slug
                                   from products as p
                                     inner join object_contexts as context on (p.context_id = context.id)
                                     inner join object_forms as f on (f.id = p.form_id)
                                     inner join object_shadows as s on (s.id = p.shadow_id)
                                     left join product_sku_links_view as link on (link.product_id = p.id)
                                     left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
                                   where p.id = new.id;
  return null;
end;
$$ language plpgsql;


create or replace function update_products_search_view_from_attrs_fn() returns trigger as $$
begin

  update products_search_view set
    product_id = subquery.product_id,
    title = subquery.title,
    description = subquery.description,
    active_from = subquery.active_from,
    active_to = subquery.active_to,
    tags = subquery.tags,
    external_id = subquery.external_id,
    archived_at = subquery.archived_at,
    slug = subquery.slug
  from (select
          p.id,
          p.slug as slug,
          f.id as product_id,
          illuminate_text(f, s, 'title') as title,
          illuminate_text(f, s, 'description') as description,
          illuminate_text(f, s, 'activeFrom') as active_from,
          illuminate_text(f, s, 'activeTo') as active_to,
          illuminate_text(f, s, 'tags') as tags,
          to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
          illuminate_text(f, s, 'externalId') as external_id,
          p.scope as scope
        from products as p
          inner join object_forms as f on (f.id = p.form_id)
          inner join object_shadows as s on (s.id = p.shadow_id)
        where p.id = new.id) as subquery
  where subquery.id = products_search_view.id;

  return null;
end;
$$ language plpgsql;

update products
set slug = generate_slug(illuminate_text(f, s, 'title'))
from object_shadows as s
  inner join object_forms as f on s.form_id = f.id
where products.shadow_id = s.id;

create or replace function generate_product_slug_from_product_insert_fn()
  returns trigger as $$
begin
  if (new.slug <> '') is not true
  then
    new.slug := (select generate_slug(illuminate_text(f, s, 'title')) as slug
                 from
                   object_shadows as s inner join
                   object_forms as f on f.id = s.form_id
                 where new.shadow_id = s.id);
  end if;
  return new;
end;
$$ language plpgsql;

drop trigger if exists generate_slugs_on_product_insert
on products;

create trigger generate_slugs_on_product_insert
before insert or update on products
for each row
execute procedure generate_product_slug_from_product_insert_fn();

alter table products alter column slug set not null;
