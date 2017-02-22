create or replace function update_returns_search_view_from_returns_insert_fn() returns trigger as $$
   begin
           insert into returns_search_view (
               id,
               reference_number,
               state,
               order_id,
               customer)
           select distinct on (new.id)
               new.id as id,
               new.reference_number as reference_number,
               new.state as state,
               new.order_id as order_id,
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

create or replace function update_returns_view_from_line_items_fn() returns trigger as $$
declare affected_cord_ref text;
begin
  update returns_search_view set
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
          from returns as o
          left join order_line_items as oli on (o.reference_number = oli.cord_ref)
          left join skus as sku on (oli.sku_id = sku.id)
          left join object_forms as sku_form on (sku.form_id = sku_form.id)
          left join object_shadows as sku_shadow on (oli.sku_shadow_id = sku_shadow.id)
          where o.reference_number = new.reference_number
          group by o.id) as subquery
      where returns_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_returns_search_view_from_returns_insert on returns;

create trigger update_returns_search_view_from_returns_insert
after insert on returns
for each row
execute procedure update_returns_search_view_from_returns_insert_fn();