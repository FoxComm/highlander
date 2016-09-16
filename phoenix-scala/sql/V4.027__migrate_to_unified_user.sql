--TODO remove customer table
--TODO remove store_admins table
--TODO remove customer_password_resets table

create table customer_users
(
    id serial primary key,
    user_id integer not null references users(id) on update restrict on delete restrict,
    account_id integer not null references accounts(id) on update restrict on delete restrict,

    is_guest boolean default false not null,

    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);


create table store_admin_users
(
    id serial primary key,
    user_id integer not null references users(id) on update restrict on delete restrict,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    state generic_string,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);

create table user_password_resets(
  id serial primary key,
  account_id integer not null references accounts(id) on update restrict on delete restrict,
  email generic_string,
  state reset_password_state not null default 'initial',
  code generic_string not null unique,
  created_at generic_timestamp,
  activated_at timestamp without time zone
);

create unique index user_password_resets__m_idx
  on user_password_resets (email,account_id,state)
  where state = 'initial';

create index user_password_resets__account_idx on user_password_resets (email,account_id);


-- from customer_id to account_id

alter table save_for_later rename customer_id to account_id;
alter table carts rename customer_id to account_id;
alter table orders rename customer_id to account_id;
alter table coupon_customer_usages rename customer_id to account_id;
alter table addresses rename customer_id to account_id;
alter table credit_cards rename customer_id to account_id;
alter table credit_cards rename gateway_customer_id to gateway_account_id;
alter table gift_cards rename customer_id to account_id;
alter table store_credits rename customer_id to account_id;
alter table returns rename customer_id to account_id;
alter table returns rename message_to_customer to message_to_account;
alter table export_orders rename customer_id to account_id;
alter table export_store_credits  rename customer_id to account_id;
alter table customers_search_view add column account_id int;
alter table store_credits_search_view rename customer_id to account_id;

--drop old constraints to customer and store_admin tables
alter table notes drop constraint notes_store_admin_id_fkey;
alter table addresses drop constraint addresses_customer_id_fkey;

alter table gift_cards drop constraint gift_cards_customer_id_fkey;
alter table returns drop constraint returns_customer_id_fkey;
alter table save_for_later drop constraint save_for_later_customer_id_fkey;
alter table coupon_customer_usages drop constraint coupon_customer_usages_customer_id_fkey;
alter table customer_password_resets drop constraint customer_password_resets_customer_id_fkey;
alter table credit_cards drop constraint credit_cards_customer_id_fkey;


-------------------------------------------------------------
--- below are migrations from old system without roles to new
-------------------------------------------------------------
create or replace function bootstrap_oms(sid int) returns void as $$
begin
    insert into resources(name, description, actions, system_id) values 
        ('cart', '', ARRAY['c', 'r', 'u', 'd'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('order', '', ARRAY['c', 'r', 'u', 'd'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('my:cart', '', ARRAY['r', 'u'], sid);
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_mdl(sid int) returns void as $$
declare
    summary_id integer;
begin
    insert into resources(name, description, actions, system_id) values 
        ('summary', '', ARRAY['r', 'u', 'c'], sid) returning id into summary_id;
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_pim(sid int) returns void as $$
begin
    insert into resources(name, description, actions, system_id) values 
        ('product', '', ARRAY['c', 'r', 'u', 'd'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('sku', '', ARRAY['c', 'r', 'u', 'd'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('album', '', ARRAY['c', 'r', 'u', 'd'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('coupon', '', ARRAY['c', 'r', 'u', 'd'], sid);
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_usr(sid int) returns void as $$
declare
    user_id integer;
    role_id integer;
    org_id integer;
begin
    insert into resources(name, description, actions, system_id) values 
        ('user', '', ARRAY['c', 'r', 'u', 'd'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('role', '', ARRAY['c', 'r', 'u', 'd'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('org', '', ARRAY['c', 'r', 'u', 'd'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('my:info', '', ARRAY['r', 'u'], sid);
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_organizations() returns void as $$
declare 
    root_scope_id integer;
    fox_org_id integer;
    merch_scope_id integer;
    merch_id integer;
    fox_admin_id integer;
    merch_admin_id integer;
    customer_id integer;
    cart_id integer;
    order_id integer;
    product_id integer;
    sku_id integer;
    album_id integer;
    coupon_id integer;
    user_id integer;
    org_id integer;
    my_cart_id integer;
    my_info_id integer;

begin

    insert into scopes(source, parent_path) values ('org', text2ltree('')) returning id into root_scope_id;
    insert into scopes(source, parent_id, parent_path) values ('org', root_scope_id,
        text2ltree(root_scope_id::text)) returning id into merch_scope_id;

    insert into organizations(name, kind, parent_id, scope_id) values 
        ('fox', 'tenant', null, root_scope_id) returning id into fox_org_id;

    insert into organizations(name, kind, parent_id, scope_id) values 
        ('merchant', 'merchant', fox_org_id, merch_scope_id) returning id into merch_id;

    insert into scope_domains(scope_id, domain) values (root_scope_id, 'foxcommerce.com');
    insert into scope_domains(scope_id, domain) values (merch_scope_id, 'merchant.com');

    select id from resources where name='cart' into cart_id;
    select id from resources where name='order' into order_id;
    select id from resources where name='product' into product_id;
    select id from resources where name='sku' into sku_id;
    select id from resources where name='album' into album_id;
    select id from resources where name='coupon' into coupon_id;
    select id from resources where name='user' into user_id;
    select id from resources where name='org' into org_id;
    select id from resources where name='my:cart' into my_cart_id;
    select id from resources where name='my:info' into my_info_id;

    insert into roles(name, scope_id) values('tenant_admin', root_scope_id) returning id into fox_admin_id; 

    perform add_perm(fox_admin_id, root_scope_id, cart_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(fox_admin_id, root_scope_id, order_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(fox_admin_id, root_scope_id, product_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(fox_admin_id, root_scope_id, sku_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(fox_admin_id, root_scope_id, album_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(fox_admin_id, root_scope_id, coupon_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(fox_admin_id, root_scope_id, user_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(fox_admin_id, root_scope_id, org_id, ARRAY['c', 'r', 'u', 'd']);

    insert into roles(name, scope_id) values ('merchant_admin', merch_scope_id) returning id into merch_admin_id;

    perform add_perm(merch_admin_id, merch_scope_id, cart_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, order_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, product_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, sku_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, album_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, coupon_id, ARRAY['c', 'r', 'u', 'd']);
    perform add_perm(merch_admin_id, merch_scope_id, user_id, ARRAY['c', 'r', 'u', 'd']);

    insert into roles(name, scope_id) values ('customer', merch_scope_id) returning id into customer_id;

    perform add_perm(customer_id, merch_scope_id, my_cart_id, ARRAY['r', 'u']);
    perform add_perm(customer_id, merch_scope_id, my_info_id, ARRAY['r', 'u']);

end;
$$ LANGUAGE plpgsql;

create or replace function add_perm(rold_id integer, scope_id integer, resource_id integer, actions text[]) returns void as $$
declare 
    permission_id integer;
    parent_path exts.ltree;
    scope_path generic_string;
    frn_str generic_string;
begin
    select scopes.parent_path from scopes where id = scope_id into parent_path;
    if parent_path = null then
        scope_path:= scope_id::generic_string;
    else
        scope_path:= ltree2text(parent_path || scope_id::text);
    end if;

    select 'frn' || ':' || systems.name || ':' || resources.name || ':' || scope_path
        from resources 
        join systems on (systems.id = resources.system_id) 
        where resources.id = resource_id 
        into frn_str;

    insert into permissions(scope_id, resource_id, frn, actions) values (scope_id, resource_id, frn_str, actions) returning id into permission_id;
    insert into role_permissions(permission_id, role_id) values (permission_id, rold_id);
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_single_merchant_system() returns int as $$
declare
    oms_id integer;
    mdl_id integer;
    pim_id integer;
    obj_id integer;
    usr_id integer;
    aut_id integer;
begin
    insert into systems(name, description) values ('oms', 'Order Management System') returning id into oms_id;
    insert into systems(name, description) values ('mdl', 'Middlewarehouse') returning id into mdl_id;
    insert into systems(name, description) values ('pim', 'Pim/Merge') returning id into pim_id;
    insert into systems(name, description) values ('usr', 'User Management') returning id into usr_id;

    perform bootstrap_oms(oms_id);
    perform bootstrap_mdl(mdl_id);
    perform bootstrap_pim(pim_id);
    perform bootstrap_usr(usr_id);

    perform bootstrap_organizations();

    return 1;
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_new_system(users bigint) returns void as $$
begin
    --we will bootstrap these using the user service
    -- existing users implies old customers and admin tables
    if users = 0 then
        raise notice 'No admins, not bootstrapping permissions';
        return;
    end if;
    perform bootstrap_single_merchant_system();
end;
$$ LANGUAGE plpgsql;


create or replace function migrate_admin(s store_admins) returns int as $$
declare 
    account_id integer;
    user_id integer;
begin
    insert into accounts(name, ratchet, created_at, updated_at, deleted_at) 
        values (s.name, s.ratchet, s.created_at, s.updated_at, s.deleted_at) 
        returning id into account_id;

    insert into account_access_methods(account_id, name, hashed_password, algorithm, created_at, updated_at, disabled_at)
        values (account_id, 'login', s.hashed_password, 0, s.created_at, s.updated_at, s.deleted_at);

    insert into users(account_id, email, name, phone_number, created_at, updated_at, deleted_at)
        values (account_id, s.email, s.name, s.phone_number, s.created_at, s.updated_at, s.deleted_at)
        returning id into user_id;
    insert into store_admin_users(user_id, account_id, state, created_at, updated_at, deleted_at)
        values( user_id, account_id, s.state, s.created_at, s.updated_at, s.deleted_at);

    perform assign_org(account_id, 'merchant');
    perform assign_role(account_id, 'merchant_admin');

    update assignments set store_admin_id = account_id where store_admin_id = s.id;
    update notes set store_admin_id = account_id where store_admin_id = s.id;
    update notes set reference_id = account_id where reference_id = s.id;
    update reasons set store_admin_id = account_id where store_admin_id = s.id;
    update notification_subscriptions set admin_id = account_id where admin_id = s.id;
    update returns set store_admin_id = account_id where store_admin_id = s.id;
    update shared_searches set store_admin_id = account_id where store_admin_id = s.id;
    update shared_search_associations set store_admin_id = account_id where store_admin_id = s.id;

    return account_id;
end;
$$ LANGUAGE plpgsql;

create or replace function migrate_customer(c customers) returns int as $$
declare 
    account_id integer;
    user_id integer;
begin
    insert into accounts(name, ratchet, created_at, updated_at, deleted_at) 
        values (c.name, c.ratchet, c.created_at, c.updated_at, c.deleted_at) 
        returning id into account_id;

    insert into account_access_methods(account_id, name, hashed_password, algorithm, created_at, updated_at, disabled_at)
        values (account_id, 'login', c.hashed_password, 0, c.created_at, c.updated_at, c.deleted_at);

    insert into users(account_id, email, name, phone_number, is_disabled, is_blacklisted, blacklisted_by, created_at, updated_at, deleted_at)
        values (account_id, c.email, c.name, c.phone_number, c.is_disabled, c.is_blacklisted, c.blacklisted_by, c.created_at, c.updated_at, c.deleted_at)
        returning id into user_id;

    insert into customer_users(user_id, account_id, is_guest, created_at, updated_at, deleted_at)
        values( user_id, account_id, c.is_guest, c.created_at, c.updated_at, c.deleted_at);

    perform assign_org(account_id, 'merchant');
    perform assign_role(account_id, 'customer');

    -- update all customer_id to account_id

    update notes set reference_id = account_id where reference_id = s.id;
    update save_for_later set account_id = account_id where account_id = c.id;
    update carts set account_id = account_id where account_id = c.id;
    update orders set account_id = account_id where account_id = c.id;
    update coupon_customer_usages set account_id = account_id where account_id = c.id;
    update addresses set account_id = account_id where account_id = c.id;
    update credit_cards set account_id = account_id where account_id = c.id;
    update gift_cards set account_id = account_id where account_id = c.id;
    update store_credits set account_id = account_id where account_id = c.id;
    update returns set account_id = account_id where account_id = c.id;
    update customers_search_view set account_id = account_id where id = c.id;
    update store_credits_search_view set account_id = account_id where account_id = c.id;

    return account_id;
end;
$$ LANGUAGE plpgsql;

create or replace function assign_org(account_id int, org_name text) returns void as $$
declare
    org_id integer;
begin
    select id from organizations where organizations.name = org_name into org_id;
    insert into account_organizations(account_id, organization_id) values (account_id, org_id);
end;
$$ LANGUAGE plpgsql;

create or replace function assign_role(account_id int, role_name text) returns void as $$
declare
    role_id integer;
begin
    select id from roles where roles.name = role_name into role_id;
    insert into account_roles(account_id, role_id) values (account_id, role_id);
end;
$$ LANGUAGE plpgsql;


select bootstrap_new_system(count(*)) from customers;
select migrate_admin(s) from store_admins as s;
select migrate_customer(c) from customers as c;

drop function migrate_admin(store_admins);
drop function migrate_customer(customers);

---update table indices

--carts
drop index customer_has_only_one_cart;
create unique index customer_has_only_one_cart on carts (account_id);

--orders
drop index orders_customer_and_state_idx;
create index orders_customer_and_state_idx on orders (account_id, state);

--addresses
drop index addresses_customer_id_idx;
create index addresses_account_id_idx on addresses (account_id);

drop index address_shipping_default_idx;
create unique index address_shipping_default_idx on addresses (account_id, is_default_shipping)
    where is_default_shipping = true;

--credit cards
drop index credit_cards_customer_id_idx;
create index credit_cards_account_id_idx on credit_cards (account_id);

drop index credit_cards_in_wallet_idx;
create index credit_cards_in_wallet_idx on credit_cards (account_id, in_wallet);

drop index credit_cards_default_idx;
create unique index credit_cards_default_idx on credit_cards (account_id, is_default, in_wallet)
    where is_default = true and in_wallet = true;

--store credits
drop index store_credits_idx;
create index store_credits_idx on store_credits (account_id, state);

--save for later
drop index save_for_later_customer_sku;
create unique index save_for_later_customer_sku on save_for_later (account_id, sku_id);

--update functions and triggers and materialized views

alter table export_customers add account_id integer;

drop materialized view notes_customers_view cascade;
create materialized view notes_customers_view as
select
    n.id,
    -- Customer
    case when count(c) = 0
    then
        null
    else
        to_json((
            c.id,
            u.account_id,
            u.name,
            u.email,
            u.is_blacklisted,
            to_char(u.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
        )::export_customers)
    end as customer
from notes as n
inner join customer_users as c on (n.reference_id = c.account_id AND n.reference_type = 'customer')
inner join users as u on (u.account_id = n.reference_id)
group by n.id, c.id, u.account_id, u.name, u.email, u.is_blacklisted, u.created_at;

create materialized view notes_search_view as
select distinct on (n.id)
    -- Note
    n.id as id,
    n.reference_id as reference_id,
    n.reference_type as reference_type,
    n.body as body,
    n.priority as priority,
    to_char(n.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    to_char(n.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as deleted_at,
    admins.store_admin as author,
    -- OneOf optional entity
    orders.order,
    customers.customer,
    gift_cards.gift_card,
    skus.sku as sku_item,
    products.product,
    promotions.promotion,
    coupons.coupon
from notes as n
inner join notes_admins_view as admins on (n.id = admins.id)
inner join notes_orders_view as orders on (n.id = orders.id)
inner join notes_customers_view as customers on (n.id = customers.id)
inner join notes_gift_cards_view as gift_cards on (n.id = gift_cards.id)
inner join notes_skus_view as skus on (n.id = skus.id)
inner join notes_products_view as products on (n.id = products.id)
inner join notes_promotions_view as promotions on (n.id = promotions.id)
inner join notes_coupons_view as coupons on (n.id = coupons.id)
order by id;

create unique index notes_search_view_idx on notes_search_view (id);

create unique index notes_customers_view_idx on notes_customers_view (id);

create or replace function update_customers_view_from_customers_insert_fn() returns trigger as $$
    begin
        insert into customers_search_view select distinct on (new.id)
            -- customer
            new.id as id,
            new.account_id as account_id,
            new.name as name,
            new.email as email,
            new.is_disabled as is_disabled,
            new.is_guest as is_guest,
            new.is_blacklisted as is_blacklisted,
            new.phone_number as phone_number,
            new.location as location,
            new.blacklisted_by as blacklisted_by,
            new.blacklisted_reason as blacklisted_reason,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as joined_at
            from customer_users as c;
      return null;
  end;
$$ language plpgsql;

drop trigger if exists update_customers_view_from_customers_insert on customers;

create trigger update_customers_view_from_customers_insert
    after insert on customer_users
    for each row
    execute procedure update_customers_view_from_customers_insert_fn();


create or replace function update_customers_view_from_customers_update_fn() returns trigger as $$
begin
    update customers_search_view set
        name = new.name,
        email = new.email,
        is_disabled = new.is_disabled,
        is_guest = new.is_guest,
        is_blacklisted = new.is_blacklisted,
        phone_number = new.phone_number,
        location = new.location,
        blacklisted_by = new.blacklisted_by,
        blacklisted_reason = new.blacklisted_reason,        
        joined_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where id = new.id;
    return null;
    end;
$$ language plpgsql;

drop trigger if exists update_customers_view_from_customers_update on customers;
create trigger update_customers_view_from_customers_update
    after update on customer_users
    for each row
    execute procedure update_customers_view_from_customers_update_fn();

create or replace function update_customers_view_from_shipping_addresses_fn() returns trigger as $$
declare account_ids integer[];
begin
  case tg_table_name
    when 'addresses' then
      account_ids := array_agg(new.account_id);
    when 'regions' then
      select array_agg(o.account_id) into strict account_ids
      from addresses as a
      inner join regions as r on (r.id = a.region_id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(o.account_id) into strict account_ids
      from addresses as a
      inner join regions as r1 on (r1.id = a.region_id)
      inner join countries as c1 on (r1.country_id = c1.id)
      where c1.id = new.id;
  end case;

  update customers_search_view set
    shipping_addresses_count = subquery.count,
    shipping_addresses = subquery.addresses
    from (select
            c.id,
            count(a) as count,
            case when count(a) = 0
            then
                '[]'
            else
                json_agg((
                  a.address1, 
                  a.address2, 
                  a.city, 
                  a.zip, 
                  r1.name, 
                  c1.name, 
                  c1.continent, 
                  c1.currency
                )::export_addresses)::jsonb
            end as addresses
        from customer_users as c
        left join addresses as a on (a.account_id = c.account_id)
        left join regions as r1 on (a.region_id = r1.id)
        left join countries as c1 on (r1.country_id = c1.id)
        where c.id = any(account_ids)
        group by c.id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_customers_view_from_orders_fn() returns trigger as $$
begin
    update customers_search_view set
        order_count = subquery.order_count,
        orders = subquery.orders
        from (select
                c.id,
                count(o.id) as order_count,
                case when count(o) = 0
                  then
                    '[]'
                else
                  json_agg((
                    o.account_id,
                    o.reference_number,
                    o.state,
                    to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                    o.sub_total,
                    o.shipping_total,
                    o.adjustments_total,
                    o.taxes_total,
                    o.grand_total,
                    0 -- FIXME
                  )::export_orders)::jsonb
                end as orders
              from customer_users as c
              left join orders as o on (c.account_id = o.account_id)
              left join order_line_items as oli on (oli.cord_ref = o.reference_number)
              where c.id = new.account_id
              group by c.id, o.id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_customers_view_from_billing_addresses_fn() returns trigger as $$
declare account_ids integer[];
begin
  case tg_table_name
    when 'credit_cards' then
      account_ids := array_agg(new.account_id);
    when 'regions' then
      select array_agg(cc.account_id) into strict account_ids
      from credit_cards as cc
      inner join regions as r on (cc.region_id = r.id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(o.account_id) into strict account_ids
      from credit_cards as cc
      inner join regions as r on (cc.region_id = r.id)
      inner join countries as c on (c.id = r.country_id)
      where c.id = new.id;
  end case;

  update customers_search_view set
    billing_addresses_count = subquery.count,
    billing_addresses = subquery.addresses
    from (select
            c.id,
            count(cc) as count,
            case when count(cc) = 0
            then
                '[]'
            else
                json_agg((
                  cc.address1, 
                  cc.address2, 
                  cc.city, 
                  cc.zip, 
                  r2.name, 
                  c2.name, 
                  c2.continent, 
                  c2.currency
                )::export_addresses)::jsonb
            end as addresses
        from customer_users as c 
        left join credit_cards as cc on (cc.account_id = c.account_id)
        left join regions as r2 on (cc.region_id = r2.id)
        left join countries as c2 on (r2.country_id = c2.id)
        where c.id = any(account_ids)
        group by c.id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_customers_view_from_customers_fn() returns trigger as $$
begin
  update customers_search_view set
    store_credit_count = subquery.count,
    store_credit_total = subquery.total
    from (select
            c.id,
            count(sc.id) as count,
            coalesce(sum(sc.available_balance), 0) as total
        from customer_users as c
        left join store_credits as sc on c.account_id = sc.account_id
        group by c.id) as subquery
    where customers_search_view.id = subquery.id;
    return null;
end;
$$ language plpgsql;

create or replace function update_orders_view_from_customers_ranking_fn() returns trigger as $$
declare
  cord_refs text[];
  account_ids int[];
begin

   case tg_table_name
     when 'orders' then
       cord_refs := array_agg(new.reference_number::text);
     when 'order_payments' then
       cord_refs := array_agg(new.cord_ref);
     when 'credit_card_charges' then
       select array_agg(op.cord_ref) into strict cord_refs
         from credit_card_charges as ccp
         inner join order_payments as op on (op.id = ccp.order_payment_id)
         where ccp.id = new.id;
     when 'gift_card_adjustments' then
       select array_agg(op.cord_ref) into strict cord_refs
         from gift_card_adjustments as gca
         inner join order_payments as op on (op.id = gca.order_payment_id)
         where gca.id = new.id;
     when 'store_credit_adjustments' then
       select array_agg(op.cord_ref) into strict cord_refs
         from store_credit_adjustments as sca
         inner join order_payments as op on (op.id = sca.order_payment_id)
         where sca.id = new.id;
     when 'returns' then
       cord_refs := array_agg(new.order_ref);
     when 'return_payments' then
       select array_agg(returns.order_ref) into strict cord_refs
       from return_payments as rp
       inner join returns on (rp.return_id = returns.id)
       where rp.id = new.id;
   end case;

  select array_agg(c.id) into strict account_ids
    from orders
    inner join customers as c on (orders.account_id = c.id)
    where orders.reference_number = any(cord_refs);


  update orders_search_view set
    customer =
       -- TODO: uncomment when jsonb_set in 9.5 will be available
       -- jsonb_set(customer, '{revenue}', jsonb (subquery.revenue::varchar), true)
       json_build_object(
               'id', customer ->> 'id',
               'account_id', customer ->> 'account_id',
               'name', customer ->> 'name',
               'email', customer ->> 'email',
               'is_blacklisted', customer ->> 'is_blacklisted',
               'joined_at', customer ->> 'joined_at',
               'rank', customer ->> 'rank',
               'revenue', subquery.revenue
           )::jsonb
      from (
            select
                c.id,
                coalesce(sum(CCc.amount),0) + coalesce(sum(SCa.debit), 0) + coalesce(sum(GCa.debit),0) - coalesce(sum(rp.amount),0) as revenue
            from customer_users as c
              inner join orders on(c.account_id = orders.account_id and orders.state in ('remorseHold', 'fulfillmentStarted', 'shipped'))
              inner join order_payments as op on(op.cord_ref = orders.reference_number)
              left join credit_card_charges as CCc on(CCc.order_payment_id = op.id and CCc.state in ('auth', 'fullCapture'))
              left join store_credit_adjustments as SCa on(SCA.order_payment_id = op.id and SCa.state in ('auth', 'capture'))
              left join gift_card_adjustments as GCa on (GCa.order_payment_id = op.id and GCa.state in ('auth', 'capture'))
              left join returns as r on (r.order_ref = orders.reference_number and r.state = 'complete')
              left join return_payments as rp on (rp.return_id = r.id and rp.amount is not null)
            where is_guest = false and c.id = any(account_ids)
              group by (c.id)
              order by revenue desc)
            as subquery
    where orders_search_view.customer ->> 'account_id' = subquery.account_id::varchar;

    return null;
end;
$$ language plpgsql;

drop materialized view failed_authorizations_search_view;
create materialized view failed_authorizations_search_view as
select distinct on (ccc.id)
    -- Credit Card Charge
    ccc.id,
    ccc.charge_id,
    ccc.amount,
    ccc.currency,
    ccc.state,
    to_char(ccc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Credit Card
    cc.holder_name,
    cc.last_four,
    cc.exp_month,
    cc.exp_year,
    cc.brand,
    -- Billing address
    cc.address1,
    cc.address2,
    cc.city,
    cc.zip,
    r.name as region,
    c.name as country,
    c.continent,
    -- Order
    o.reference_number as cord_reference_number,
    -- Customer
    o.account_id as account_id
from credit_card_charges as ccc
inner join credit_cards as cc on (ccc.credit_card_id = cc.id)
inner join regions as r on (cc.region_id = r.id)
inner join countries as c on (r.country_id = c.id)
inner join order_payments as op on (op.id = ccc.order_payment_id)
inner join orders as o on (op.cord_ref = o.reference_number)
where ccc.state = 'failedAuth'
order by ccc.id;

drop materialized view order_stats_view cascade;
create materialized view order_stats_view as
select
	o.id,
    o.account_id,
	o.reference_number,
	o.state,
	o.placed_at,
	o.sub_total,
	o.shipping_total,
	o.adjustments_total,
	o.taxes_total,
	o.grand_total,
    count(oli.id) as items_count
from orders as o
left join order_line_items as oli on o.reference_number = oli.cord_ref
group by o.id;

create materialized view notes_orders_view as
select
    n.id,
    -- Order
    case when count(osv) = 0
    then
        '[]'
    else
        json_agg((
        	osv.account_id,
        	osv.reference_number,
        	osv.state,
        	to_char(osv.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        	osv.sub_total,
        	osv.shipping_total,
        	osv.adjustments_total,
        	osv.taxes_total,
        	osv.grand_total,
        	osv.items_count
        )::export_orders)
    end as order
from notes as n
left join order_stats_view as osv on (n.reference_id = osv.id AND n.reference_type = 'order')
group by n.id;

drop materialized view store_credit_transactions_view;
create materialized view store_credit_transactions_view as
select distinct on (sca.id)
    -- Store Credit Transaction
    sca.id,
    sca.debit,
    sca.available_balance,
    sca.state,
    to_char(sca.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    -- Store Credit
    sc.id as store_credit_id,
    sc.account_id,
    sc.origin_type,
    sc.currency,
    to_char(sc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as store_credit_created_at,
    -- Order Payment
    sctpv.order_payment,
    -- Store admins
    sctav.store_admin
from store_credit_adjustments as sca
inner join store_credits as sc on (sc.id = sca.store_credit_id)
inner join store_credit_transactions_payments_view as sctpv on (sctpv.id = sca.id)
inner join store_credit_transactions_admins_view as sctav on (sctav.id = sca.id)
order by sca.id;

drop materialized view gift_card_from_store_credits_view;
create materialized view gift_card_from_store_credits_view as
select
    gc.id,
    -- Gift cards
    case when count(sc) = 0
    then
        null
    else
        to_json((sc.id, sc.account_id, sc.origin_type, sc.currency, to_char(sc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_store_credits)
    end as store_credit
from gift_cards as gc
left join gift_card_from_store_credits as gcfsc on (gc.origin_id = gcfsc.id)
left join store_credits as sc on (sc.id = gcfsc.store_credit_id)
group by gc.id, sc.id;

drop materialized view customer_save_for_later_view cascade;
create materialized view customer_save_for_later_view as
select
    s.id,
    -- Customer
    later.account_id as account_id,
    u.name as customer_name,
    u.email as customer_email,
    -- SKU
    s.code as sku_code,
    f.attributes->>(sh.attributes->'title'->>'ref') as sku_title,
    f.attributes->(sh.attributes->'salePrice'->>'ref')->>'value' as sku_price,
    -- Save for later
    to_char(later.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as saved_for_later_at
from save_for_later as later
inner join customer_users as c on later.account_id = c.account_id
inner join users as u on u.account_id = c.account_id
inner join skus as s on later.sku_id = s.id
inner join object_forms as f on f.id = s.form_id
inner join object_shadows as sh on sh.id = s.shadow_id;

create unique index customer_save_for_later_view_idx on customer_save_for_later_view (id, account_id);

create or replace function update_store_credits_view_insert_fn() returns trigger as $$
    begin
        insert into store_credits_search_view select distinct on (new.id)
            new.id as id,
            new.account_id as account_id,
            new.origin_id as origin_id,
            new.origin_type as origin_type,           
            new.state as state,
            new.currency as currency,
            new.original_balance as original_balance,
            new.current_balance as current_balance,
            new.available_balance as available_balance,
            new.canceled_amount as canceled_amount,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
            to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at
            from store_credits as sc;
      return null;
  end;
$$ language plpgsql;

create or replace function update_store_credits_view_update_fn() returns trigger as $$
begin
    update store_credits_search_view set
        account_id = new.account_id,
        origin_id = new.origin_id,
        origin_type = new.origin_type,
        state = new.state,
        currency = new.currency,
        original_balance = new.original_balance,
        available_balance = new.available_balance,
        current_balance = new.current_balance,
        canceled_amount = new.canceled_amount,
        created_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        updated_at = to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where id = new.id;
    return null;
    end;
$$ language plpgsql;

alter table carts drop constraint customer_id_fk;
alter table orders drop constraint customer_id_fk;
alter table store_credits drop constraint customer_id_fk;
alter table orders drop constraint object_context_id_fk;

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
    o.account_id as account_id,
    u.name as customer_name,
    u.email as customer_email,
    -- SKU
    s.code as sku_code,
    f.attributes->>(sh.attributes->'title'->>'ref') as sku_title,
    f.attributes->(sh.attributes->'salePrice'->>'ref')->>'value' as sku_price
from order_line_items as oli
inner join orders as o on o.reference_number = oli.cord_ref and o.state = 'shipped'
inner join customer_users as c on o.account_id = c.account_id
inner join users as u on u.account_id = o.account_id
inner join skus as s on oli.sku_id = s.id
inner join object_forms as f on f.id = s.form_id
inner join object_shadows as sh on sh.id = oli.sku_shadow_id
where oli.state = 'shipped';

create unique index customer_purchased_items_view_idx on customer_purchased_items_view (id, account_id, reference_number);

create materialized view customer_items_view as
select
	nextval('customer_items_view_seq') as id,
	-- Customer
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

create or replace function update_customers_view_revenue_fn() returns trigger as $$
begin
    update customers_search_view set 
        revenue = subquery.revenue 
        from (
			select
				c.id as account_id,
		    	coalesce(sum(ccc.amount), 0) + coalesce(sum(sca.debit), 0) + coalesce(sum(gca.debit), 0) as revenue
		    from customer_users as c
		    inner join orders on (c.account_id = orders.account_id and orders.state = 'shipped')
		    inner join order_payments as op on (op.cord_ref = orders.reference_number)
		    left join credit_card_charges as ccc on (ccc.order_payment_id = op.id and ccc.state = 'fullCapture')
		    left join store_credit_adjustments as sca on (sca.order_payment_id = op.id and sca.state = 'capture')
		    left join gift_card_adjustments as gca on (gca.order_payment_id = op.id and gca.state = 'capture')
		    where is_guest = false and c.account_id = new.account_id
		    group by c.id
		    order by c.id) as subquery
    where customers_search_view.id = subquery.account_id;
    return null;
end;
$$ language plpgsql;

create or replace function update_customers_ranking() returns boolean as $$
begin
	 -- Update customers ranks
      update customers_search_view
        set rank = q.rank from (
            select
              c.id,
              c.revenue,
              ntile(100) over (w) as rank
            from
              customers_search_view as c
            where revenue > 0
              window w as (order by c.revenue desc)
              order by revenue desc) as q
          where customers_search_view.id = q.id;

      -- Update Carts
      update carts_search_view set
        customer = q.customer from (
          select
              cs.id,
              c.rank,
              jsonb_set(jsonb_set(cs.customer, '{rank}', jsonb (c.rank::varchar), true),
                        '{revenue}', jsonb (c.revenue::varchar), true)
                        as customer
          from carts_search_view as cs
          inner join customers_search_view as c on (c.id = (cs.customer->>'id')::bigint)
          where c.rank > 0
        ) as q
        where carts_search_view.id = q.id;

      -- Update Orders
      update orders_search_view set
        customer = q.customer from (
          select
              o.id,
              c.rank,
              jsonb_set(jsonb_set(o.customer, '{rank}', jsonb (c.rank::varchar), true),
                        '{revenue}', jsonb (c.revenue::varchar), true)
                      as customer
          from orders_search_view as o
          inner join customers_search_view as c on (c.id = (o.customer->>'id')::bigint)
          where c.rank > 0
        ) as q
        where orders_search_view.id = q.id;

	return true;
end;
$$ language plpgsql;

drop index orders_search_view_customer_idx;
create index orders_search_view_account_idx on orders_search_view((customer->>'account_id'));

create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
    begin
        insert into orders_search_view select distinct on (new.id)
            -- order
            new.id as id,
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
                'account_id', c.account_id,
                'name', c.name,
                'email', c.email,
                'is_blacklisted', c.is_blacklisted,
                'joined_at', c.joined_at,
                'rank', c.rank,
                'revenue', c.revenue
            )::jsonb as customer
            from customers_search_view as c
            where (new.account_id = c.account_id);
        return null;
    end;
$$ language plpgsql;


create or replace function update_carts_view_from_carts_insert_fn() returns trigger as $$
    begin
        insert into carts_search_view select distinct on (new.id)
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
                'account_id', c.account_id,
                'name', c.name,
                'email', c.email,
                'is_blacklisted', c.is_blacklisted,
                'joined_at', c.joined_at,
                'rank', c.rank,
                'revenue', c.revenue
            )::jsonb as customer
            from customers_search_view as c
            where (new.account_id = c.account_id);
        return null;
    end;
$$ language plpgsql;

create or replace function update_orders_view_from_customers_fn() returns trigger as $$
begin
    update orders_search_view set
        customer = json_build_object(
            'id', new.id,
            'account_id', new.account_id,
            'name', new.name,
            'email', new.email,
            'is_blacklisted', new.is_blacklisted,
            'joined_at', to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
            'rank', customer ->> 'rank',
            'revenue', customer ->> 'revenue'
        )::jsonb where customer ->> 'account_id' = new.account_id::varchar;
    return null;
end;
$$ language plpgsql;

drop trigger if exists update_orders_view_from_customers on customers;
create trigger update_orders_view_from_customers
    after update on customer_users
    for each row
    execute procedure update_orders_view_from_customers_fn();


--drop customers and store admins

drop table customer_password_resets;
drop table customers;
drop table store_admins;
