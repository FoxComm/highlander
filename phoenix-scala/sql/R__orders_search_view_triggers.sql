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
