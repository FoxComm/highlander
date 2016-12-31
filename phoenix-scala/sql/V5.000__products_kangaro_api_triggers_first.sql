-- R__carts_search_view
create or replace function update_carts_view_from_carts_insert_fn() returns trigger as $$
begin
  insert into carts_search_view(
    id,
    reference_number,
    created_at,
    updated_at,
    currency,
    sub_total,
    shipping_total,
    adjustments_total,
    taxes_total,
    grand_total,
    customer,
    scope) select distinct on (new.id)
                                  -- order
                                  new.id as id,
                                  new.reference_number as reference_number,
                                  to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
                                  to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
                                  new.currency as currency,
                                  -- totals
                                  new.sub_total as sub_total,
                                  new.shipping_total as shipping_total,
                                  new.adjustments_total as adjustments_total,
                                  new.taxes_total as taxes_total,
                                  new.grand_total as grand_total,
                                  -- customer
                                  json_build_object(
                                      'id', c.id,
                                      'name', c.name,
                                      'email', c.email,
                                      'is_blacklisted', c.is_blacklisted,
                                      'joined_at', c.joined_at,
                                      'rank', c.rank,
                                      'revenue', c.revenue
                                  )::jsonb as customer,
                                  new.scope as scope
                                from customers_search_view as c
                                where (new.account_id = c.id);
  return null;
end;
$$ language plpgsql;

create or replace function update_carts_view_from_line_items_fn() returns trigger as $$
declare cord_refs text[];
begin
  case tg_table_name
    when 'cart_line_items' then
      cord_refs := array_agg(new.cord_ref);
    when 'product_variants' then
      select array_agg(cord_ref) into strict cord_refs
        from cart_line_items as cli
        where cli.product_variant_id = new.id;
    when 'object_forms' then
      select array_agg(cord_ref) into strict cord_refs
      from cart_line_items as cli
        inner join product_variants as variant on (cli.product_variant_id = variant.id)
        where variant.form_id = new.id;
  end case;

  update carts_search_view set
    line_item_count = subquery.count,
    line_items = subquery.items from (select
          c.id,
          count(variant.id) as count,
          case when count(variant) = 0
          then
            '[]'
          else
            json_agg((
                    cli.reference_number,
                    'cart',
                    variant.code,
                    vform.attributes->>(vshadow.attributes->'title'->>'ref'),
                    vform.attributes->>(vshadow.attributes->'externalId'->>'ref'),
                    vform.attributes->(vshadow.attributes->'salePrice'->>'ref')->>'value',
                    cli.attributes,
                    variant.scope)::export_line_items)
                    ::jsonb
          end as items
          from carts as c
          left join cart_line_items as cli on (c.reference_number = cli.cord_ref)
          left join product_variants as variant on (cli.product_variant_id = variant.id)
          left join object_forms as vform on (variant.form_id = vform.id)
          left join object_shadows as vshadow on (variant.shadow_id = vshadow.id)
          where c.reference_number = any(cord_refs)
          group by c.id) as subquery
      where carts_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

-- R__orders_search_views

create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
    begin
        insert into orders_search_view (
            id,
            scope,
            reference_number,
            state,
            placed_at,
            currency,
            sub_total,
            shipping_total,
            adjustments_total,
            taxes_total,
            grand_total,
            customer)
        select distinct on (new.id)
            -- order
            new.id as id,
            new.scope as scope,
            new.reference_number as reference_number,
            new.state as state,
            to_char(new.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as placed_at,
            new.currency as currency,
            -- totals
            new.sub_total as sub_total,
            new.shipping_total as shipping_total,
            new.adjustments_total as adjustments_total,
            new.taxes_total as taxes_total,
            new.grand_total as grand_total,
            -- customer
            json_build_object(
                'id', c.id,
                'name', c.name,
                'email', c.email,
                'is_blacklisted', c.is_blacklisted,
                'joined_at', c.joined_at,
                'rank', c.rank,
                'revenue', c.revenue
            )::jsonb as customer
        from customers_search_view as c
        where (new.account_id = c.id);
        return null;
    end;
$$ language plpgsql;

create or replace function update_orders_view_from_line_items_fn() returns trigger as $$
declare affected_cord_ref text;
begin
  case tg_table_name
    when 'order_line_items' then
      affected_cord_ref := new.cord_ref;
    when 'orders_search_view' then
      affected_cord_ref := new.reference_number;
  end case;
  update orders_search_view set
    line_item_count = subquery.count,
    line_items = subquery.items from (select
          o.id,
          count(variant.id) as count,
          case when count(variant) = 0
          then
            '[]'
          else
            json_agg((
                    oli.reference_number,
                    oli.state,
                    variant.code,
                    illuminate_text(vform, vshadow, 'title'),
                    vform.attributes->>(vshadow.attributes->'externalId'->>'ref'),
                    vform.attributes->(vshadow.attributes->'salePrice'->>'ref')->>'value',
                    oli.attributes,
                    variant.scope)::export_line_items)
                    ::jsonb
          end as items
          from orders as o
          left join order_line_items as oli on (o.reference_number = oli.cord_ref)
          left join product_variants as variant on (oli.product_variant_id = variant.id)
          left join object_forms as vform on (variant.form_id = vform.id)
          left join object_shadows as vshadow on (oli.product_variant_shadow_id = vshadow.id)
          where o.reference_number = affected_cord_ref
          group by o.id) as subquery
      where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

-- R__product_sku_links_view

create or replace function get_skus_for_product(int) returns jsonb as $$
-- return list of sku codes from variants from product
declare
    skus jsonb;
begin
    select
      case when count(pvariant) = 0
        then
            '[]'::jsonb
        else
        json_agg(pvariant.code)::jsonb
      end into skus
    from product_to_variant_links as link
    inner join product_variants as pvariant on (pvariant.id = link.right_id)
    where link.left_id = $1;

  if (skus = '[]'::jsonb) then
    select
      case when count(pvariants) = 0
        then
            '[]'::jsonb
        else
        json_agg(distinct pvariants.code)::jsonb
      end into skus
      from product_to_option_links as polink
        inner join product_option_to_value_links as vvlink on (polink.right_id = vvlink.left_id)
        inner join product_value_to_variant_links as vsku_link on (vsku_link.left_id = vvlink.right_id)
        inner join product_variants as pvariants on (vsku_link.right_id = pvariants.id)
      where polink.left_id = $1;
  end if;

  return skus;

end;
$$ language plpgsql;


create or replace function insert_product_sku_links_view_from_products_fn() returns trigger as $$
begin

  insert into product_to_variant_links_view select
    new.id as product_id,
    get_skus_for_product(new.id) as skus;

    return null;
end;
$$ language plpgsql;


create or replace function update_product_sku_links_view_from_products_and_deps_fn() returns trigger as $$
declare product_ids int[];
begin
  case tg_table_name
    when 'products' then
      product_ids := array_agg(new.id);
    when 'product_to_variant_links' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_to_variant_links as link on (link.left_id = p.id)
      where link.id = new.id;
    when 'product_variants' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_to_variant_links as link on link.left_id = p.id
      inner join product_variants as sku on (sku.id = link.right_id)
      where sku.id = new.id;
    when 'product_value_to_variant_links' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_to_option_links as polink on (polink.left_id = p.id)
      inner join product_value_to_variant_links as vvlink on (polink.right_id = vvlink.left_id)
      where vvlink.right_id = (case TG_OP
                            when 'DELETE' then
                              old.left_id
                            else
                              new.left_id
                          end);
  end case;

  update product_to_variant_links_view set
    skus = subquery.skus
    from (select
            p.id,
            get_skus_for_product(p.id) skus
          from products as p
         where p.id = any(product_ids)
         group by p.id) as subquery
    where subquery.id = product_to_variant_links_view.product_id;
    return null;
end;
$$ language plpgsql;

-- R__sku_search_view_triggers

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
      inner join object_shadows as shadow on (shadow.id = variant.shadow_id)
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
        variants.id as product_variant_id,
        to_json_timestamp(variants.archived_at) as archived_at
      from object_contexts as o
      inner join product_variants as variants on (variants.context_id = o.id)
      where variants.id = new.id) as subquery
      where subquery.product_variant_id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_skus_view_image_fn() returns trigger as $$
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

  update sku_search_view
    set image = subquery.image
  from (select variant.id as id,
               variant.code as code,
               (product_album_links_view.albums #>> '{0, images, 0, src}') as image
        from product_variants as variant
          inner join product_to_variant_links on variant.id = product_to_variant_links.right_id
          inner join product_album_links_view on product_to_variant_links.left_id = product_album_links_view.product_id
        where variant.id = any(product_variant_ids)) as subquery
  where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;

-- R__notes_search_view_triggers

create or replace function update_notes_search_view_insert_fn() returns trigger as $$
declare new_note notes_search_view%rowtype;
begin

  select distinct on (new.id) into strict new_note
    new.id as id,
    new.reference_id as reference_id,
    new.reference_type as reference_type,
    new.body as body,
    new.priority as priority,
    to_json_timestamp(n.created_at) as created_at,
    to_json_timestamp(n.deleted_at) as deleted_at,
    to_json((users.email, users.name)::export_store_admins) as author,
    new.scope as scope
  from notes as n
    inner join users on (n.store_admin_id = users.account_id)
  where n.id = new.id;

  case new.reference_type
    when 'order' then
    select
      case cords.is_cart
      when false then
        json_agg((
                   o.customer->>'id',
                   o.reference_number,
                   o.state,
                   o.placed_at,
                   o.sub_total,
                   o.shipping_total,
                   o.adjustments_total,
                   o.taxes_total,
                   o.grand_total,
                   o.line_item_count
                 )::export_orders)
      when true then
        json_agg((
                   carts.customer->>'id',
                   carts.reference_number,
                   null,
                   null,
                   carts.sub_total,
                   carts.shipping_total,
                   carts.adjustments_total,
                   carts.taxes_total,
                   carts.grand_total,
                   carts.line_item_count
                 )::export_orders)
      end into strict new_note.order
    from cords
      left join orders_search_view as o on (o.reference_number = cords.reference_number and cords.is_cart = false)
      left join carts_search_view as carts on (carts.reference_number = cords.reference_number and cords.is_cart = true)
    where cords.id = new.reference_id
    group by cords.is_cart;
    when 'customer' then
    select
      to_json((
                c.id,
                u.account_id,
                u.name,
                u.email,
                u.is_blacklisted,
                to_json_timestamp(u.created_at)
              )::export_customers) into strict new_note.customer
    from users as u
      inner join customer_data as c on (u.id = c.user_id)
    where u.account_id = new.reference_id;
    when 'giftCard' then
    select
      to_json((gc.code, gc.origin_type, gc.currency, to_json_timestamp(gc.created_at))::export_gift_cards)
    into strict new_note.gift_card
    from gift_cards as gc
    where gc.id = new.reference_id;
    when 'variant' then
    select distinct on(v.form_id)
      to_json((
                f.id,
                v.code,
                to_json_timestamp(v.created_at)
              )::export_skus_raw)
    into strict new_note.variant_item
    from product_variants as v
      inner join object_forms as f on (f.id = v.form_id and f.kind = 'variant')
    where v.form_id = new.reference_id;
    when 'product' then
    select distinct on(p.form_id)
      to_json((
                f.id,
                to_json_timestamp(p.created_at)
              )::export_products_raw)
    into strict new_note.product
    from products as p
      inner join object_forms as f on (f.id = p.form_id and f.kind = 'product')
    where p.form_id = new.reference_id;

    when 'promotion' then
    select distinct on(p.form_id)
      to_json((
                f.id,
                p.apply_type,
                to_json_timestamp(p.created_at)
              )::export_promotions_raw)
    into strict new_note.promotion
    from promotions as p
      inner join object_forms as f on (f.id = p.form_id and f.kind = 'promotion')
    where p.form_id = new.reference_id;
    when 'coupon' then
    select distinct on(c.form_id)
      to_json((
                f.id,
                c.promotion_id,
                to_json_timestamp(c.created_at)
              )::export_coupons_raw)
    into strict new_note.coupon
    from coupons as c
      inner join object_forms as f on (f.id = c.form_id and f.kind = 'coupon')
    where c.form_id = new.reference_id;
    when 'storeAdmin' then
    select
      to_json((users.email, users.name)::export_store_admins)
    into strict new_note.store_admin
    from users
    where users.account_id = new.reference_id;
  end case;

  insert into notes_search_view select new_note.*;

  return null;
end;
$$ language plpgsql;

-- variant
create or replace function update_notes_search_view_on_sku_fn() returns trigger as $$
begin

  update notes_search_view set variant_item = q.sku from
          (select
            to_json((
                f.id,
                v.code,
                to_json_timestamp(v.created_at)
            )::export_skus_raw)
              as sku
            from product_variants as v
            inner join object_forms as f on (f.id = v.form_id and f.kind = 'variant')
            where v.form_id = new.form_id) as q
        where notes_search_view.reference_id = new.form_id and notes_search_view.reference_type = 'variant';

  return null;
end;
$$ language plpgsql;

