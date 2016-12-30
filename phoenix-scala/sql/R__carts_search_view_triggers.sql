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