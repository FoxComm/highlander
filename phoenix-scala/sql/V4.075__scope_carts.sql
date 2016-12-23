
alter table carts add column scope exts.ltree;
alter table carts_search_view add column scope exts.ltree;
alter table export_line_items add column scope exts.ltree;

update carts set scope = exts.text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update carts_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

alter table carts alter column scope set not null;

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
    when 'skus' then
      select array_agg(cord_ref) into strict cord_refs
        from cart_line_items as cli
        where cli.sku_id = new.id;
    when 'object_forms' then
      select array_agg(cord_ref) into strict cord_refs
      from cart_line_items as cli
        inner join skus as sku on (cli.sku_id = sku.id)
        where sku.form_id = new.id;
  end case;

  update carts_search_view set
    line_item_count = subquery.count,
    line_items = subquery.items from (select
          c.id,
          count(sku.id) as count,
          case when count(sku) = 0
          then
            '[]'
          else
            json_agg((
                       cli_skus.reference_number,
                       'cart',
                    sku.code,
                    sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref'),
                    sku_form.attributes->>(sku_shadow.attributes->'externalId'->>'ref'),
                    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value',
                    cli_skus.attributes,
                    sku.scope)::export_line_items)
                    ::jsonb
          end as items
          from carts as c
          left join cart_line_items as cli_skus on (c.reference_number = cli_skus.cord_ref)
          left join skus as sku on (cli_skus.sku_id = sku.id)
          left join object_forms as sku_form on (sku.form_id = sku_form.id)
          left join object_shadows as sku_shadow on (sku.shadow_id = sku_shadow.id)
          where c.reference_number = any(cord_refs)
          group by c.id) as subquery
      where carts_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;
