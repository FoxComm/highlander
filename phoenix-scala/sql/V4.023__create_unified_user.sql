create table organization_type
(
    id integer primary key not null,
    name varchar(255)
);

create table organization_domains
(
    id integer primary key not null,
    organization_id integer not null references organizations(id) on update restrict on delete restrict
    domain generic_string not null, --used to tie users to an organization if the registration
                                    --page requires an organization
);

create table organizations
(
    id integer primary key not null,
    name varchar(255) not null,
    organization_type_id integer,
    parent_id integer default null,
    constraint organizations_organization_type_id_fk foreign key (organization_type_id) references organization_type (id)
);

create table permissions
(
    id integer primary key not null,
    resource_id integer not null,
    action_id integer not null,
    scope_id varchar(255),
    constraint permissions_resources_id_fk foreign key (resource_id) references resources (id),
    constraint permissions_actions_id_fk foreign key (action_id) references actions (id),
    constraint permissions_scopes_id_fk foreign key (scope_id) references scopes (id)
);

create table role_permissions
(
    id integer default nextval('role_permissions_id_seq'::regclass) not null,
    permission_id integer not null,
    role_id integer not null,
    constraint role_permissions_permissions_id_fk foreign key (permission_id) references permissions (id),
    constraint role_permissions_roles_id_fk foreign key (role_id) references 
);

create unique index role_permissions_id_permission_id_role_id_uindex on role_permissions (id, permission_id, role_id);

--These are the legal scopes in the system
--Scopes are used to create claims for roles. 
create table scopes
(
    id integer primary key not null,
    path exts.ltree not null
);

create table roles
(
    id integer primary key not null
    name varchar(255) not null,
    scope_id integer references scopes(id) on update restrict on delete restrict
);

create table account_roles
(
    id integer primary key not null,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    role_id integer not null references roles(id) on update restrict on delete restrict
);

create table accounts
(
    id integer primary key not null,
    name generic_string,
    ratchet integer not null,
);

-- Accounts may have many access methods which includes a hashed password. 
-- normal users will have passwords while service accounts will have api keys.
-- API keys have a key id and key secret.
create table account_access_methods
(
    id integer primary key not null,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    name generic_string,
    hashed_password generic_string not null, --This is computed with scrypt which includes salt.
    disabled_at timestamp without time zone null
);

create table account_organizations
(
    id integer primary key,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    organization_id integer not null references organizations(id) on update restrict on delete restrict
);

create table users
(
    id integer primary key,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    email email not null,
    is_disabled boolean not null default false,
    disabled_by integer null,
    name generic_string,
    phone_number phone_number,
    is_blacklisted boolean not null default false,
    blacklisted_by integer null,
    blacklisted_reason generic_string,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null,
    foreign key (disabled_by) references store_admins(id) on update restrict on delete restrict
);

create table service
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

create table customers
(
    id integer primary key,
    user_id integer not null references users(id) on update restrict on delete restrict,
    account_id integer not null references accounts(id) on update restrict on delete restrict,

    location generic_string,
    modality generic_string,
    is_guest boolean default false not null
);


create table store_admins
(
    id integer primary key,
    user_id integer not null references users(id) on update restrict on delete restrict,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    state generic_string,
);


