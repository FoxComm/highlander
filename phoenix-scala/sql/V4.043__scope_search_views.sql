create or replace function bootstrap_demo_organization(org_name text, org_com text, parent_org_id integer, parent_scope_id integer) returns int as $$
declare 
    merch_scope_id integer;
    merch_id integer;
    merch_admin_id integer;
    customer_id integer;
    cart_id integer;
    order_id integer;
    product_id integer;
    sku_id integer;
    album_id integer;
    coupon_id integer;
    user_id integer;
    org_id integer;
    my_cart_id integer;
    my_info_id integer;

begin

    insert into scopes(source, parent_id, parent_path) values ('org', parent_scope_id,
        text2ltree(parent_scope_id::text)) returning id into merch_scope_id;

    insert into organizations(name, kind, parent_id, scope_id) values 
        (org_name, 'merchant', parent_org_id, merch_scope_id) returning id into merch_id;

    insert into scope_domains(scope_id, domain) values (merch_scope_id, org_com);

    select id from resources where name='cart' into cart_id;
    select id from resources where name='order' into order_id;
    select id from resources where name='product' into product_id;
    select id from resources where name='sku' into sku_id;
    select id from resources where name='album' into album_id;
    select id from resources where name='coupon' into coupon_id;
    select id from resources where name='user' into user_id;
    select id from resources where name='org' into org_id;
    select id from resources where name='my:cart' into my_cart_id;
    select id from resources where name='my:info' into my_info_id;

    insert into roles(name, scope_id) values ('admin', merch_scope_id) returning id into merch_admin_id;

    perform add_perm(merch_admin_id, merch_scope_id, cart_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, order_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, product_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, sku_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, album_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, coupon_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, user_id, ARRAY['c', 'r', 'u', 'd']);

    insert into roles(name, scope_id) values ('customer', merch_scope_id) returning id into customer_id;

    perform add_perm(customer_id, merch_scope_id, my_cart_id, ARRAY['r', 'u']);
    perform add_perm(customer_id, merch_scope_id, my_info_id, ARRAY['r', 'u']);

    return 1;
end;
$$ LANGUAGE plpgsql;

--adding scope to search views based on object store
create or replace function illuminate_text(form object_forms, shadow object_shadows, key text) returns text as $$
begin
    return form.attributes->>(shadow.attributes->key->>'ref');
end
$$ LANGUAGE plpgsql;

create or replace function illuminate_obj(form object_forms, shadow object_shadows, key text) returns jsonb as $$
begin
    return form.attributes->(shadow.attributes->key->>'ref');
end
$$ LANGUAGE plpgsql;


alter table products_catalog_view add column scope exts.ltree not null;
alter table album_search_view add column scope exts.ltree not null;
alter table coupons_search_view add column scope exts.ltree not null;
alter table promotions_search_view add column scope exts.ltree not null;
alter table products_search_view add column scope exts.ltree not null;
alter table sku_search_view add column scope exts.ltree not null;

update products_catalog_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update album_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update coupons_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update promotions_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update products_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update sku_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

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
    (p.archived_at is not null and (p.archived_at)::timestamp < current_timestamp)
    or
    ((illuminate_text(f, s, 'activeFrom')) = '') is not false
    or
    (illuminate_text(f, s, 'activeFrom'))::timestamp >= current_timestamp
    or
      (((illuminate_text(f, s, 'activeTo')) = '') IS FALSE and
      ((illuminate_text(f, s, 'activeTo'))::timestamp < current_timestamp)));

    -- add new products (inactive -> active transition)
    select array_agg(p.id) into insert_ids
    from products as p
      left join products_catalog_view as pv on (pv.id = p.id)
      inner join object_forms as f on (f.id = p.form_id)
      inner join object_shadows as s on (s.id = p.shadow_id)
    where pv.id is null and -- check that product not in products_catalog_view yet
        (p.archived_at is null or (p.archived_at)::timestamp > current_timestamp) and
        (illuminate_text(f, s, 'activeFrom'))::timestamp < current_timestamp and
        (((illuminate_text(f, s, 'activeTo')) = '') is not false or
        ((illuminate_text(f, s, 'activeTo'))::timestamp >= current_timestamp));

    if array_length(insert_ids, 1) > 0 then
      insert into products_catalog_view select
        p.id,
        f.id as product_id,
        context.name as context,
        illuminate_text(f, s, 'title') as title,
        illuminate_text(f, s, 'description') as description,
        sku.sale_price as sale_price,
        sku.sale_price_currency as currency,
        illuminate_text(f, s, 'tags') as tags,
        albumLink.albums as albums,
        p.scope as scope
        from products as p
          inner join object_contexts as context on (p.context_id = context.id)
          inner join object_forms as f on (f.id = p.form_id)
          inner join object_shadows as s on (s.id = p.shadow_id)
          inner join product_sku_links_view as sv on (sv.product_id = p.id)
          inner join sku_search_view as sku on (sku.context_id = context.id and sku.sku_code = sv.skus->>0)
          left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
        where p.id = any(insert_ids);
      end if;

return true;
end;
$$ language plpgsql;

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
      illuminate_text(f, s, 'title') as title,
      illuminate_text(f, s, 'description') as description,
      sku.sale_price as sale_price,
      sku.sale_price_currency as currency,
      illuminate_text(f, s, 'tags') as tags,
      albumLink.albums as albums,
      p.scope as scope
      from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.sku_code = sv.skus->>0)
        left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
      where p.id = any(insert_ids) and
            (p.archived_at is null or (p.archived_at)::timestamp > current_timestamp) and
            (illuminate_text(f, s, 'activeFrom'))::timestamp < current_timestamp and
            (((illuminate_text(f, s, 'activeTo')) = '') is not false or
            ((illuminate_text(f, s, 'activeTo'))::timestamp >= current_timestamp));
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
            illuminate_text(f, s, 'title') as title,
            illuminate_text(f, s, 'description') as description,
            sku.sale_price as sale_price,
            sku.sale_price_currency as currency,
            illuminate_text(f, s, 'tags') as tags,
            albumLink.albums as albums,
            to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
            p.scope as scope
      from products as p
        inner join object_contexts as context on (p.context_id = context.id)
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join product_sku_links_view as sv on (sv.product_id = p.id)--get list of sku codes for the product
        inner join sku_search_view as sku on (sku.context_id = context.id and sku.sku_code = sv.skus->>0)
        left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
      where p.id = any(update_ids) and
            (p.archived_at is null or (p.archived_at)::timestamp > current_timestamp) and
            (illuminate_text(f, s, 'activeFrom'))::timestamp < current_timestamp and
            (((illuminate_text(f, s, 'activeTo')) = '') is not false or
            ((illuminate_text(f, s, 'activeTo'))::timestamp >= current_timestamp)))
      as subquery
  where products_catalog_view.id = subquery.id;
  end if;

  return null;
end;
$$ language plpgsql;

create or replace function update_albums_view_from_object_attrs_fn()
  returns trigger as $$
declare album_ids int [];
begin
  case tg_table_name
    when 'object_shadows'
    then
      select array_agg(albums.id) into strict album_ids
      from albums inner join object_shadows as album_shadow on (albums.shadow_id = album_shadow.id)
      where album_shadow.id = new.id;
    when 'object_forms'
    then
      select array_agg(albums.id) into strict album_ids
      from albums inner join object_forms as album_form on (albums.form_id = album_form.id)
      where album_form.id = new.id;
    when 'albums'
    then
      select array_agg(albums.id) into strict album_ids
      from albums where albums.id = new.id;
    when 'album_image_links'
    then
      case TG_OP
        when 'DELETE' then
          select array_agg(old.left_id) into strict album_ids;
      else
          select array_agg(new.left_id) into strict album_ids;
      end case;
  end case;

  update album_search_view
  set
    name        = subquery.name,
    images      = subquery.images,
    archived_at = subquery.archived_at
  from (select
          album.id,
          album_form.attributes ->> (album_shadow.attributes -> 'name' ->> 'ref') as name,
          get_images_json_for_album(album.id)                                     as images,
          to_char(album.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')             as archived_at,
          album.scope as scope
        from albums as album
          inner join object_forms as album_form on (album_form.id = album.form_id)
          inner join object_shadows as album_shadow on (album_shadow.id = album.shadow_id)
        where album.id = any (album_ids)) as subquery
  where subquery.id = album_search_view.album_id;

  return null;
end;
$$ language plpgsql;

create or replace function insert_albums_view_from_albums_fn() returns trigger as $$
begin
  insert into album_search_view 
    select
      new.id as album_id,
      context.name as context,
      context.id as context_id,
      album_form.attributes ->> (album_shadow.attributes -> 'name' ->> 'ref') as name,
      get_images_json_for_album(new.id) as images,
      new.archived_at as archived_at,
      new.scope as scope
    from
      object_contexts as context
        left join object_shadows as album_shadow on (album_shadow.id = new.shadow_id)
        left join object_forms as album_form on (album_form.id = new.form_id)
    where
      context.id = new.context_id;

  return null;
end;
$$ language plpgsql;

create or replace function update_coupons_view_insert_fn() returns trigger as $$
  begin
    insert into coupons_search_view select distinct on (new.id)
      f.id,
      cp.promotion_id,
      context.name as context,
      illuminate_text(f, s, 'name') as name,
      illuminate_text(f, s, 'storefrontName') as storefront_name,
      illuminate_text(f, s, 'description') as description,
      illuminate_text(f, s, 'activeFrom') as active_from,
      illuminate_text(f, s, 'activeTo') as active_to,
      0 as total_used,
      to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
      to_char(cp.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
      cp.scope as scope
      from coupons as cp
      inner join object_contexts as context on (cp.context_id = context.id)
      inner join object_forms as f on (f.id = cp.form_id)
      inner join object_shadows as s on (s.id = cp.shadow_id)
      where cp.id = new.id;

      return null;
  end;
$$ language plpgsql;


create or replace function update_coupons_view_update_fn() returns trigger as $$
begin
    update coupons_search_view set
      id = q.id,
      promotion_id = q.promotion_id,
      context = q.context,
      name = q.name,
      storefront_name = q.storefront_name,
      description = q.description,
      active_from = q.active_from,
      active_to = q.active_to,
      created_at = q.created_at,
      archived_at = q.archived_at
      from (select
              f.id,
              cp.promotion_id,
              cp.form_id,
              context.name as context,
              illuminate_text(f, s, 'name') as name,
              illuminate_text(f, s, 'storefrontName') as storefront_name,
              illuminate_text(f, s, 'description') as description,
              illuminate_text(f, s, 'activeFrom') as active_from,
              illuminate_text(f, s, 'activeTo') as active_to,
              to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
              to_char(cp.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
              cp.scope as scope
            from coupons as cp
            inner join object_contexts as context on (cp.context_id = context.id)
            inner join object_forms as f on (f.id = cp.form_id)
            inner join object_shadows as s on (s.id = cp.shadow_id)
            where cp.id = new.id) as q
    where coupons_search_view.id = q.form_id;
    return null;
    end;
$$ language plpgsql;

create or replace function update_promotions_disc_links_view_insert_fn() returns trigger as $$
  begin
    insert into promotion_discount_links_view select distinct on (new.id)
        p.form_id as promotion_id,
        context.id as context_id,
        -- discounts
        case when count(discount) = 0
          then
            '[]'::jsonb
        else
            json_agg(discount.form_id)::jsonb
        end as discounts
      from promotions as p
      inner join object_contexts as context on (p.context_id = context.id)
      inner join discounts as discount on (discount.id = new.right_id)
      where p.id = new.left_id
      group by p.form_id, context.id;

      return null;
  end;
$$ language plpgsql;

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
    p.scope as scope
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

-- object forms
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
    archived_at = subquery.archived_at
    from (select
            p.id,
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
    image = subquery.image,
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
        illuminate_text(sku_form, sku_shadow, 'title') as title,
        illuminate_obj(sku_form, sku_shadow, 'images')->>0 as image,
        illuminate_obj(sku_form, sku_shadow, 'salePrice')->>'value' as sale_price,
        illuminate_obj(sku_form, sku_shadow, 'salePrice')->>'currency' as sale_price_currency,
        to_char(new.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
        illuminate_obj(sku_form, sku_shadow, 'retailPrice')->>'value' as retail_price,
        illuminate_obj(sku_form, sku_shadow, 'retailPrice')->>'currency' as retail_price_currency,
        illuminate_obj(sku_form, sku_shadow, 'externalId') as external_id,
        sku.scope as scope
      from skus as sku
      inner join object_forms as sku_form on (sku_form.id = sku.form_id)
      inner join object_shadows as sku_shadow on (sku_shadow.id = sku.shadow_id)
      where sku.id = new.id) as subquery
      where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_promotions_search_view_insert_fn() returns trigger as $$
begin
 insert into promotions_search_view select distinct on (p.id)
     f.id,
     context.name as context,
     p.apply_type,
     illuminate_text(f, s, 'name') as promotion_name,
     illuminate_text(f, s, 'storefrontName') as storefront_name,
     illuminate_text(f, s, 'description') as description,
     illuminate_text(f, s, 'activeFrom') as active_from,
     illuminate_text(f, s, 'activeTo') as active_to,
     0 as total_used, --this needs to be computed
     0 as current_carts, --this needs to be computed
     to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
     to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
     new.discounts as discounts,
     p.scope as scope
   from promotions as p
     inner join object_forms as f on (f.id = p.form_id)
     inner join object_shadows as s on (s.id = p.shadow_id)
     inner join object_contexts as context on (p.context_id = context.id and new.context_id = context.id)
   where new.promotion_id = p.form_id;

return null;
end;
$$ language plpgsql;

create or replace function update_promotions_view_update_from_self_fn() returns trigger as $$
begin
  update promotions_search_view set
    apply_type = q.apply_type,
    context = q.context,
    promotion_name = q.promotion_name,
    storefront_name = q.storefront_name,
    description = q.description,
    active_from = q.active_from,
    active_to = q.active_to,
    total_used = q.total_used,
    current_carts = q.current_carts,
    created_at = q.created_at,
    archived_at = q.archived_at
      from (select
          p.form_id as promotion_form_id,
          f.id,
          context.name as context,
          p.apply_type,
          illuminate_text(f, s, 'name') as promotion_name,
          illuminate_text(f, s, 'storefrontName') as storefront_name,
          illuminate_text(f, s, 'description') as description,
          illuminate_text(f, s, 'activeFrom') as active_from,
          illuminate_text(f, s, 'activeTo') as active_to,
          0 as total_used, --this needs to be computed
          0 as current_carts, --this needs to be computed
          to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
          to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
          p.scope as scope
        from promotions as p
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join object_contexts as context on (p.context_id = context.id)
      where new.id = p.id) as q
    where promotions_search_view.id = q.promotion_form_id;

  return null;
end;
$$ language plpgsql;
