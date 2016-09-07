
create table customer_users
(
    id serial primary key,
    user_id integer not null references users(id) on update restrict on delete restrict,
    account_id integer not null references accounts(id) on update restrict on delete restrict,

    blacklisted_by integer null,
    blacklisted_reason generic_string,

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

-------------------------------------------------------------
--- below are migrations from old system without roles to new
-------------------------------------------------------------
create or replace function bootstrap_oms(sid int) returns void as $$
begin
    insert into resources(name, description, actions, system_id) values 
        ('cart', '', ARRAY['r', 'w', 'create', 'delete'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('order', '', ARRAY['r', 'w', 'create', 'delete'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('my:cart', '', ARRAY['r', 'w'], sid);
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_mdl(sid int) returns void as $$
declare
    summary_id integer;
begin
    insert into resources(name, description, actions, system_id) values 
        ('summary', '', ARRAY['r', 'w', 'create'], sid) returning id into summary_id;
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_pim(sid int) returns void as $$
begin
    insert into resources(name, description, actions, system_id) values 
        ('product', '', ARRAY['r', 'w', 'create', 'archive'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('sku', '', ARRAY['r', 'w', 'create', 'archive'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('album', '', ARRAY['r', 'w', 'create', 'archive'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('coupon', '', ARRAY['r', 'w', 'create', 'archive'], sid);
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_usr(sid int) returns void as $$
declare
    user_id integer;
    role_id integer;
    org_id integer;
begin
    insert into resources(name, description, actions, system_id) values 
        ('user', '', ARRAY['r', 'w', 'create', 'disable'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('role', '', ARRAY['r', 'w', 'create', 'delete'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('org', '', ARRAY['r', 'w', 'create', 'delete'], sid);
    insert into resources(name, description, actions, system_id) values 
        ('my:info', '', ARRAY['r', 'w'], sid);
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_organizations() returns void as $$
declare 
    root_scope_id integer;
    root_scope_str text;
    fox_org_id integer;
    merch_scope_id integer;
    merch_id integer;
    fox_admin_id integer;
    merch_admin_id integer;
    merch_scope_str text;
    customer_id integer;
begin

    insert into scopes(source) values ('org') returning id into root_scope_id;
    insert into scopes(source, parent_id) values ('org', root_scope_id) returning id into merch_scope_id;

    insert into organizations(name, type, parent_id, scope_id) values 
        ('fox', 'tenant', null, root_scope_id) returning id into fox_org_id;

    insert into organizations(name, type, parent_id, scope_id) values 
        ('merchant', 'merchant', fox_org_id, merch_scope_id) returning id into merch_id;

    insert into scope_domains(scope_id, domain) values (root_scope_id, 'foxcommerce.com');
    insert into scope_domains(scope_id, domain) values (merch_scope_id, 'merchant.com');

    insert into roles(name, scope_id) values('fox admin', root_scope_id) returning id into fox_admin_id; 
    insert into roles(name, scope_id) values ('merchant admin', merch_scope_id) returning id into merch_admin_id;

    select root_scope_id into root_scope_str;

    perform add_claim(fox_admin_id, root_scope_str, 'frn:oms:cart', ARRAY['r', 'w', 'create', 'delete']);
    perform add_claim(fox_admin_id, root_scope_str, 'frn:oms:order', ARRAY['r', 'w', 'create', 'delete']);
    perform add_claim(fox_admin_id, root_scope_str, 'frn:pim:product', ARRAY['r', 'w', 'create', 'archive']);
    perform add_claim(fox_admin_id, root_scope_str, 'frn:pim:sku', ARRAY['r', 'w', 'create', 'archive']);
    perform add_claim(fox_admin_id, root_scope_str, 'frn:pim:album', ARRAY['r', 'w', 'create', 'archive']);
    perform add_claim(fox_admin_id, root_scope_str, 'frn:pim:coupon', ARRAY['r', 'w', 'create', 'archive']);
    perform add_claim(fox_admin_id, root_scope_str, 'frn:usr:user', ARRAY['r', 'w', 'create', 'disable']);
    perform add_claim(fox_admin_id, root_scope_str, 'frn:usr:org', ARRAY['r', 'w', 'create', 'delete']);

    select root_scope_id || '/' || merch_scope_id into merch_scope_str;

    perform add_claim(merch_admin_id, merch_scope_str, 'frn:oms:cart', ARRAY['r', 'w', 'create', 'delete']);
    perform add_claim(merch_admin_id, merch_scope_str, 'frn:oms:order', ARRAY['r', 'w', 'create', 'delete']);
    perform add_claim(merch_admin_id, merch_scope_str, 'frn:pim:product', ARRAY['r', 'w', 'create', 'archive']);
    perform add_claim(merch_admin_id, merch_scope_str, 'frn:pim:sku', ARRAY['r', 'w', 'create', 'archive']);
    perform add_claim(merch_admin_id, merch_scope_str, 'frn:pim:album', ARRAY['r', 'w', 'create', 'archive']);
    perform add_claim(merch_admin_id, merch_scope_str, 'frn:pim:coupon', ARRAY['r', 'w', 'create', 'archive']);
    perform add_claim(merch_admin_id, merch_scope_str, 'frn:usr:info', ARRAY['r', 'w', 'create', 'disable']);

    insert into roles(name, scope_id) values ('customer', merch_scope_id) returning id into customer_id;

    perform add_claim(customer_id, merch_scope_str, 'frn:oms:my:cart', ARRAY['r', 'w']);
    perform add_claim(customer_id, merch_scope_str, 'frn:usr:my:info', ARRAY['r', 'w']);

end;
$$ LANGUAGE plpgsql;

create or replace function add_claim(rold_id int, scope text, frn generic_string, actions text[]) returns void as $$
declare 
    claim_id integer;
    final_frn generic_string;
begin
    select frn || ':' || scope into final_frn;

    insert into claims(frn, actions) values (final_frn, actions) returning id into claim_id;
    insert into role_claims(claim_id, role_id) values (claim_id, rold_id);
end;
$$ LANGUAGE plpgsql;

create or replace function bootstrap_system() returns void as $$
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
    perform bootstrap_system();
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

    insert into account_access_methods(account_id, name, hashed_password, created_at, updated_at, disabled_at)
        values (account_id, 'login', s.hashed_password, s.created_at, s.updated_at, s.deleted_at);

    insert into users(account_id, email, name, phone_number, created_at, updated_at, deleted_at)
        values (account_id, s.email, s.name, s.phone_number, s.created_at, s.updated_at, s.deleted_at)
        returning id into user_id;
    insert into store_admin_users(user_id, account_id, state, created_at, updated_at, deleted_at)
        values( user_id, account_id, s.state, s.created_at, s.updated_at, s.deleted_at);

    perform assign_org(account_id, 'merchant');
    perform assign_role(account_id, 'merchant admin');

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

    insert into account_access_methods(account_id, name, hashed_password, created_at, updated_at, disabled_at)
        values (account_id, 'login', c.hashed_password, c.created_at, c.updated_at, c.deleted_at);

    insert into users(account_id, email, name, phone_number, is_disabled, created_at, updated_at, deleted_at)
        values (account_id, c.email, c.name, c.phone_number, c.is_disabled, c.created_at, c.updated_at, c.deleted_at)
        returning id into user_id;

    insert into customer_users(user_id, account_id, blacklisted_by, blacklisted_reason, 
        is_guest, created_at, updated_at, deleted_at)
        values( user_id, account_id, c.blacklisted_by, c.blacklisted_reason, 
            c.is_guest, c.created_at, c.updated_at, c.deleted_at);

    perform assign_org(account_id, 'merchant');
    perform assign_role(account_id, 'customer');

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



