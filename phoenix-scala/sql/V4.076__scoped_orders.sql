alter table orders add column scope exts.ltree;
alter table orders_search_view add column scope exts.ltree;

update orders set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update orders_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

alter table orders alter column scope set not null;
alter table orders_search_view alter column scope set not null;

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
                    oli.attributes,
                    sku.scope)::export_line_items)
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

