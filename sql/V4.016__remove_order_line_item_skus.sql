
--add sku_id and sku_shadow_id
alter table order_line_items add sku_id integer;
alter table order_line_items add sku_shadow_id integer;

--find and assign correct sku_id and shadow_id
update order_line_items set sku_id = ols.sku_id, sku_shadow_id = ols.sku_shadow_id
    from (select id, sku_id, sku_shadow_id from order_line_item_skus) as ols
    where ols.id = origin_id;


drop materialized view customer_items_view;

drop materialized view customer_purchased_items_view;

create materialized view customer_purchased_items_view as
select
    s.id,
    oli.reference_number,
    to_char(oli.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Order
    o.reference_number as cord_reference_number,
    to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as order_placed_at,
    -- Customer
    o.customer_id as customer_id,
    c.name as customer_name,
    c.email as customer_email,
    -- SKU
    s.code as sku_code,
    f.attributes->>(sh.attributes->'title'->>'ref') as sku_title,
    f.attributes->(sh.attributes->'salePrice'->>'ref')->>'value' as sku_price
from order_line_items as oli
inner join orders as o on o.reference_number = oli.cord_ref and o.state = 'shipped'
inner join customers as c on o.customer_id = c.id
inner join skus as s on oli.sku_id = s.id
inner join object_forms as f on f.id = s.form_id
inner join object_shadows as sh on sh.id = oli.sku_shadow_id
where oli.state = 'shipped';

create unique index customer_purchased_items_view_idx on customer_purchased_items_view (id, reference_number);

refresh materialized view concurrently customer_purchased_items_view;

create materialized view customer_items_view as
select
	nextval('customer_items_view_seq') as id,
	-- Customer
	coalesce(t1.customer_id, t2.customer_id) as customer_id,
	coalesce(t1.customer_name, t2.customer_name) as customer_name,
	coalesce(t1.customer_email, t2.customer_email) as customer_email,
	-- SKU
	coalesce(t1.sku_code, t2.sku_code) as sku_code,
	coalesce(t1.sku_title, t2.sku_title) as sku_title,
	coalesce(t1.sku_price, t2.sku_price) as sku_price,
	-- Order
	coalesce(t1.cord_reference_number, null) as cord_reference_number,
	coalesce(t1.order_placed_at, null) as order_placed_at,
	-- Save for later
	coalesce(null, t2.saved_for_later_at) as saved_for_later_at	
from customer_purchased_items_view as t1
full outer join customer_save_for_later_view as t2 ON t1.id = t2.id;

create unique index customer_items_view_idx on customer_items_view (id);

refresh materialized view concurrently customer_items_view;

--remove references to order_line_item_skus
alter table order_line_items drop origin_id;
alter table order_line_items drop origin_type;

--remove order_line_item_skus table
drop table order_line_item_skus;

--we didn't really support gift card purchasing up to this point, so ignore 
--migrating data...
drop table order_line_item_gift_cards;

create or replace function update_orders_view_from_line_items_fn() returns trigger as $$
begin
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
          left join skus as sku on (oli.sku_id = sku.id)
          left join object_forms as sku_form on (sku.form_id = sku_form.id)
          left join object_shadows as sku_shadow on (oli.sku_shadow_id = sku_shadow.id)
          where o.id = new.id
          group by o.id) as subquery
      where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

drop trigger update_carts_view_from_line_items on cart_line_item_skus;
drop trigger set_cli_refnum_trg on cart_line_item_skus;

alter table cart_line_item_skus rename to cart_line_items;

create trigger set_cli_refnum_trg
before insert on cart_line_items
for each row execute procedure set_cli_refnum();

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
                    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value')::export_line_items)
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

create trigger update_carts_view_from_line_items
after update or insert on cart_line_items
for each row
execute procedure update_carts_view_from_line_items_fn();

create or replace function delete_cart_line_items() returns trigger as $$
begin
  delete from cart_line_items where cord_ref = old.reference_number;
  return null;
end;
$$ language plpgsql;
