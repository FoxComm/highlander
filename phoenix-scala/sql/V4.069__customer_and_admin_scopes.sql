alter table customer_data add column scope exts.ltree;
alter table admin_data add column scope exts.ltree;

update customer_data set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update admin_data set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

alter table customer_data alter column scope set not null;
alter table admin_data alter column scope set not null;

drop materialized view customer_items_view;
drop materialized view customer_purchased_items_view;
drop materialized view customer_save_for_later_view;

create materialized view customer_purchased_items_view as
  select
    s.id,
    c.scope as scope,
    oli.reference_number,
    to_char(oli.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Order
    o.reference_number as cord_reference_number,
    to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as order_placed_at,
    -- Customer
    o.account_id as account_id,
    u.name as customer_name,
    u.email as customer_email,
    -- SKU
    s.code as sku_code,
    illuminate_text(f, sh, 'title') as sku_title,
    illuminate_obj(f, sh, 'salePrice')->>'value' as sku_price
  from order_line_items as oli
    inner join orders as o on o.reference_number = oli.cord_ref and o.state = 'shipped'
    inner join customer_data as c on o.account_id = c.account_id
    inner join users as u on u.account_id = o.account_id
    inner join skus as s on oli.sku_id = s.id
    inner join object_forms as f on f.id = s.form_id
    inner join object_shadows as sh on sh.id = oli.sku_shadow_id
  where oli.state = 'shipped';

create unique index customer_purchased_items_view_idx on customer_purchased_items_view (id, account_id, reference_number);

create materialized view customer_save_for_later_view as
  select
    s.id,
    -- Customer
    c.scope as scope,
    later.account_id as account_id,
    u.name as customer_name,
    u.email as customer_email,
    -- SKU
    s.code as sku_code,
    illuminate_text(f, sh, 'title') as sku_title,
    illuminate_obj(f, sh, 'salePrice')->>'value' as sku_price,
    -- Save for later
    to_char(later.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as saved_for_later_at
  from save_for_later as later
    inner join customer_data as c on later.account_id = c.account_id
    inner join users as u on u.account_id = c.account_id
    inner join skus as s on later.sku_id = s.id
    inner join object_forms as f on f.id = s.form_id
    inner join object_shadows as sh on sh.id = s.shadow_id;

create unique index customer_save_for_later_view_idx on customer_save_for_later_view (id, account_id);

create materialized view customer_items_view as
  select
    nextval('customer_items_view_seq') as id,
    -- Customer
    coalesce(t1.scope, t2.scope)  as scope,
    coalesce(t1.account_id, t2.account_id) as account_id,
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

alter table customers_search_view add column scope exts.ltree;
update customers_search_view set scope = cd.scope from customer_data as cd where cd.id = customers_search_view.id;

create or replace function update_customers_view_from_customers_insert_fn() returns trigger as $$
begin
  insert into customers_search_view(id, name, email, is_disabled, is_guest, is_blacklisted, phone_number,
                                    blacklisted_by, joined_at, scope) select
                                      -- customer
                                      new.account_id as id,
                                      u.name as name,
                                      u.email as email,
                                      u.is_disabled as is_disabled,
                                      new.is_guest as is_guest,
                                      u.is_blacklisted as is_blacklisted,
                                      u.phone_number as phone_number,
                                      u.blacklisted_by,
                                      to_char(u.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as joined_at,
                                      c.scope as scope
                                    from customer_data as c, users as u
                                    where c.account_id = new.account_id and u.account_id = new.account_id;
  return null;
end;
$$ language plpgsql;

alter table store_admins_search_view add column scope exts.ltree;
update store_admins_search_view set scope = ad.scope from admin_data as ad where ad.id = store_admins_search_view.id;

create or replace function update_store_admins_view_insert_fn() returns trigger as $$
begin
  insert into store_admins_search_view (id, name, email, phone_number, state, created_at, scope)
    select
                                         u.account_id as id,
                                         u.name as name,
                                         u.email as email,
                                         u.phone_number as phone_number,
                                         new.state as state,
                                         to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
                                         new.scope as scope
                                       from admin_data as s, users as u
                                       where s.account_id = new.account_id
                                             and u.account_id = new.account_id;

  return null;
end;
$$ language plpgsql;
