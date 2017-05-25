create or replace function insert_customer_items_search_view_from_line_items_fn()
  returns trigger as $$
begin
  insert into customer_items_view
    select
      s.id as id,
      c.scope as scope,
      -- Customer
      o.account_id as customer_id,
      u.name as customer_name,
      u.email as customer_email,
      -- SKU
      s.code as sku_code,
      illuminate_text(f, sh, 'title') as sku_title,
      illuminate_obj(f, sh, 'salePrice')->>'value' as sku_price
      -- Order
      o.reference_number as order_reference_number,
      to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as order_placed_at
    from order_line_items as oli
      inner join orders as o on o.reference_number = oli.cord_ref and o.state = 'shipped'
      inner join customer_data as c on o.account_id = c.account_id
      inner join users as u on u.account_id = o.account_id
      inner join skus as s on oli.sku_id = s.id
      inner join object_forms as f on f.id = s.form_id
      inner join object_shadows as sh on sh.id = oli.sku_shadow_id
    where oli.id = new.id and oli.state = 'shipped';
  return null;
end;
$$ language plpgsql;

drop trigger if exists insert_customer_items_search_view_from_line_items on order_line_items;
create trigger insert_taxonomies_search_view_from_taxonomies
  after update on order_line_items
  for each row
    execute procedure insert_customer_items_search_view_from_line_items_fn();
