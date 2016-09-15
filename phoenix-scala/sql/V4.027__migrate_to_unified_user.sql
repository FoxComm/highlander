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
  user_id integer not null references users(id) on update restrict on delete restrict,
  email generic_string,
  state reset_password_state not null default 'initial',
  code generic_string not null unique,
  created_at generic_timestamp,
  activated_at timestamp without time zone
);

create unique index user_password_resets__m_idx
  on user_password_resets (email,user_id,state)
  where state = 'initial';

create index user_password_resets__user_idx on user_password_resets (email,user_id);


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
alter table user_password_resets rename user_id to account_id;

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

    insert into organizations(name, type, parent_id, scope_id) values 
        ('fox', 'tenant', null, root_scope_id) returning id into fox_org_id;

    insert into organizations(name, type, parent_id, scope_id) values 
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

create or replace function bootstrap_single_merchant_system() returns void as $$
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


create or replace function make_new_admin(s store_admins) returns int as $$
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
    update reasons set store_admin_id = account_id where store_admin_id = s.id;
    update notification_subscriptions set admin_id = account_id where admin_id = s.id;
    update returns set store_admin_id = account_id where store_admin_id = s.id;
    update shared_searches set store_admin_id = account_id where store_admin_id = s.id;
    update shared_search_associations set store_admin_id = account_id where store_admin_id = s.id;

    return account_id;
end;
$$ LANGUAGE plpgsql;

create or replace function make_new_customer(c customers) returns int as $$
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

    update save_for_later set account_id = account_id where account_id = c.id;
    update carts set account_id = account_id where account_id = c.id;
    update orders set account_id = account_id where account_id = c.id;
    update coupon_customer_usages set account_id = account_id where account_id = c.id;
    update addresses set account_id = account_id where account_id = c.id;
    update credit_cards set account_id = account_id where account_id = c.id;
    update gift_cards set account_id = account_id where account_id = c.id;
    update store_credits set account_id = account_id where account_id = c.id;
    update returns set account_id = account_id where account_id = c.id;
    update user_password_resets set account_id = account_id where account_id = c.id;

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
select make_new_admin(s) from store_admins as s;
select make_new_customer(c) from customers as c;





/* create table service
(
    id integer primary key,
--find and assign correct sku_id and shadow_id
update order_line_items set sku_id = ols.sku_id, sku_shadow_id = ols.sku_shadow_id
    from (select id, sku_id, sku_shadow_id from order_line_item_skus) as ols
    where ols.id = origin_id;
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    client_id generic_string,
    description generic_string
);
*/



