alter table cart_line_items add column attributes  jsonb;
alter table order_line_items add column attributes  jsonb;
alter table export_line_items add column attributes  jsonb;
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
          count(sku.id) as count,
          case when count(sku) = 0
          then
            '[]'
          else
            json_agg((
                    oli.reference_number,
                    oli.state,
                    sku.code,
                    sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref'),
                    sku_form.attributes->>(sku_shadow.attributes->'externalId'->>'ref'),
                    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value',
                    sku.scope,
                    oli.attributes)::export_line_items)
                    ::jsonb
          end as items
          from orders as o
          left join order_line_items as oli on (o.reference_number = oli.cord_ref)
          left join skus as sku on (oli.sku_id = sku.id)
          left join object_forms as sku_form on (sku.form_id = sku_form.id)
          left join object_shadows as sku_shadow on (oli.sku_shadow_id = sku_shadow.id)
          where o.reference_number = affected_cord_ref
          group by o.id) as subquery
      where orders_search_view.id = subquery.id;

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
                    sku.scope,
                    cli_skus.attributes)::export_line_items)
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
