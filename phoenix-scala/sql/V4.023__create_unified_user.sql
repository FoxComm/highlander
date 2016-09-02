--These are the legal scopes in the system
--Scopes are used to create claims for roles.
create table scopes
(
    id integer primary key not null,
    source generic_string,
    parent_id integer references scopes(id) on update restrict on delete restrict
);

create table organizations
(
    id serial primary key,
    name varchar(255) not null,
    type generic_string,
    parent_id integer default null references organizations(id) on update restrict on delete restrict,
    scope_id integer references scopes(id) on update restrict on delete restrict
);

create table scope_domains
(
    id integer primary key not null,
    scope_id integer not null references scopes(id) on update restrict on delete restrict,
    domain generic_string not null --used to tie users to an organization if the registration
                                    --page requires an organization
);

CREATE TABLE resources (
    id serial primary key not null,
    name generic_string NOT NULL,
    frn text not null, --Fox Resource Name: used to inject into the claim
    description TEXT
);
CREATE UNIQUE INDEX resources_id_uindex ON resources USING BTREE (id);

--Actions will include the base-level of read and write
--Future actions can include limited-read functionality
--Ex: Merchant viewing order with only their owned-line-items
--Every action exists for a specified resource, allowing the resource to register custom actions downstream
CREATE TABLE actions (
    id INTEGER PRIMARY KEY NOT NULL,
    name CHARACTER VARYING(255) NOT NULL,
    resource_id integer references resources(id) on update restrict on delete restrict
 );
CREATE UNIQUE INDEX actions_id_uindex ON actions USING BTREE (id);


--This is the baseline permission that an entity has.
create table permissions
(
    id integer primary key not null,
    resource_id integer not null,
    action_id integer not null,
    scope_id integer not null,
    constraint permissions_resources_id_fk foreign key (resource_id) references resources (id),
    constraint permissions_actions_id_fk foreign key (action_id) references actions (id),
    constraint permissions_scopes_id_fk foreign key (scope_id) references scopes (id)
);

--We use the permissions above to generate the claims; which are flattened/simplified representations of permissions
create table claims
(
    id serial primary key,
    --Fox Resource Name
    --The FRN includes the scope
    --TODO: Figure out top/bottom cascading rules for nested scopes.
    frn generic_string not null
);

--Roles exist at every specific level of scope.
create table roles
(
    id integer primary key not null,
    name varchar(255) not null,
    scope_id integer references scopes(id) on update restrict on delete restrict
);

create table role_claims
(
    id serial primary key,
    claim_id integer not null references claims(id) on update restrict on delete restrict,
    role_id integer not null references roles(id) on update restrict on delete restrict
);

--Role Archetypes are the generic permission-set that can be applied to any sub-scope.
--They are used to bootstap roles.
create table role_archetypes
(
    id integer primary key not null,
    name varchar(255) not null,
    scope_id integer references scopes(id) on update restrict on delete restrict
);

create table role_permissions
(
    id serial primary key not null,
    permission_id integer not null references permissions(id) on update restrict on delete restrict,
    role_id integer not null references roles(id) on update restrict on delete restrict
);

create unique index role_permissions_id_permission_id_role_id_uindex on role_permissions (id, permission_id, role_id);

create table accounts
(
    id integer primary key not null,
    name generic_string,
    ratchet integer not null
);

create table account_roles
(
    id integer primary key not null,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    role_id integer not null references roles(id) on update restrict on delete restrict
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
    disabled_at generic_timestamp_null
);

--
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
    deleted_at timestamp without time zone null
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
    state generic_string
);

alter table users add foreign key (disabled_by) references store_admins(id) on update restrict on delete restrict;

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

