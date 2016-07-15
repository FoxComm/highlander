create or replace function update_orders_view_from_line_items_fn() returns trigger as $$
declare cord_refs text[];
begin
  case tg_table_name
    when 'order_line_items' then
      cord_refs := array_agg(new.cord_ref);
    when 'order_line_item_skus' then
      select array_agg(cord_ref) into strict cord_refs
        from order_line_items as oli
        inner join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
        where oli_origins.id = new.id;
    when 'skus' then
      select array_agg(cord_ref) into strict cord_refs
        from order_line_items as oli
        inner join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
        inner join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
        where oli_skus.id = new.id;
    when 'object_forms' then
      select array_agg(cord_ref) into strict cord_refs
        from order_line_items as oli
        inner join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
        inner join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
        inner join skus as sku on (oli_skus.sku_id = sku.id)
        where sku.form_id = new.id;
    when 'object_shadows' then
      select array_agg(cord_ref) into strict cord_refs
        from order_line_items as oli
        inner join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
        inner join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
        where oli_skus.sku_shadow_id = new.id;
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
                    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value')::export_line_items)
                    ::jsonb
          end as items
          from orders as o
          left join order_line_items as oli on (o.reference_number = oli.cord_ref)
          left join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
          left join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
          left join skus as sku on (oli_skus.sku_id = sku.id)
          left join object_forms as sku_form on (sku.form_id = sku_form.id)
          left join object_shadows as sku_shadow on (oli_skus.sku_shadow_id = sku_shadow.id)
          where o.reference_number = any(cord_refs)
          group by o.id) as subquery
      where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create trigger update_orders_view_from_line_items
    after update or insert on order_line_items
    for each row
    execute procedure update_orders_view_from_line_items_fn();

create trigger update_orders_view_from_line_items
    after update or insert on order_line_item_skus
    for each row
    execute procedure update_orders_view_from_line_items_fn();

create trigger update_orders_view_from_line_items
    after update or insert on skus
    for each row
    execute procedure update_orders_view_from_line_items_fn();

create trigger update_orders_view_from_line_items
    after update or insert on object_forms
    for each row
    execute procedure update_orders_view_from_line_items_fn();

create trigger update_orders_view_from_line_items
    after update or insert on object_shadows
    for each row
    execute procedure update_orders_view_from_line_items_fn();
