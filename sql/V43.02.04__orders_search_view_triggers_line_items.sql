create or replace function update_orders_view_from_line_items_fn() returns trigger as $$
declare order_ids int[];
begin
  case TG_TABLE_NAME
    when 'order_line_items' then
      order_ids := array_agg(NEW.order_id);
    when 'order_line_item_skus' then
      select array_agg(order_id) into strict order_ids
        from order_line_items as oli
        inner join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
        where oli_origins.id = NEW.id;
    when 'skus' then
      select array_agg(order_id) into strict order_ids
        from order_line_items as oli
        inner join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
        inner join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
        WHERE oli_skus.id = NEW.id;
    when 'object_forms' then
      select array_agg(order_id) into strict order_ids
        from order_line_items as oli
        inner join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
        inner join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
        inner join skus as sku on (oli_skus.sku_id = sku.id)
        WHERE sku.form_id = NEW.id;
    when 'object_shadows' then
      select array_agg(order_id) into strict order_ids
        from order_line_items as oli
        inner join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
        inner join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
        WHERE oli_skus.sku_shadow_id = NEW.id;
  end case;

  update orders_search_view_test set
    (line_item_count, line_items) = (select
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
      end as items
      from orders as o
      left join order_line_items as oli on (o.id = oli.order_id)
      left join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
      left join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
      left join skus as sku on (oli_skus.sku_id = sku.id)
      left join object_forms as sku_form on (sku.form_id = sku_form.id)
      left join object_shadows as sku_shadow on (oli_skus.sku_shadow_id = sku_shadow.id)
      where o.id = ANY(order_ids));

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
