
--These are the legal scopes in the system
--Scopes are used to create claims for roles.
create table scopes
(
    id serial primary key,
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
    id serial primary key,
    scope_id integer not null references scopes(id) on update restrict on delete restrict,
    domain generic_string not null --used to tie users to an organization if the registration
                                    --page requires an organization
);

create table systems (
    id serial primary key,
    name generic_string not null,
    description generic_string not null
);

CREATE TABLE resources (
    id serial primary key not null,
    name generic_string NOT NULL,
    system_id integer references systems(id) on update restrict on delete restrict,
    description text,
    actions text[]
);

--This is the baseline permission that an entity has.
create table permissions
(
    id serial primary key not null,
    resource_id integer not null,
    actions text[],
    scope_id integer not null,
    constraint permissions_resources_id_fk foreign key (resource_id) references resources (id),
    constraint permissions_scopes_id_fk foreign key (scope_id) references scopes (id)
);

--We use the permissions above to generate the claims; which are flattened/simplified representations of permissions
create table claims
(
    id serial primary key,
    --Fox Resource Name
    --The FRN includes the scope
    --TODO: Figure out top/bottom cascading rules for nested scopes.
    frn generic_string not null,
    actions text[]
);

--Roles exist at every specific level of scope.
create table roles
(
    id serial primary key,
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
    id serial primary key,
    name varchar(255) not null,
    scope_id integer references scopes(id) on update restrict on delete restrict
);

create table role_archetype_permissions
(
    id serial primary key not null,
    permission_id integer not null references permissions(id) on update restrict on delete restrict,
    role_archetype_id integer not null references role_archetypes(id) on update restrict on delete restrict
);

create unique index role_permissions_id_permission_id_role_id_uindex on role_archetype_permissions (id, permission_id, role_archetype_id);

create table accounts
(
    id serial primary key,
    name generic_string,
    ratchet integer not null,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);

create table account_roles
(
    id serial primary key,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    role_id integer not null references roles(id) on update restrict on delete restrict
);

-- Accounts may have many access methods which includes a hashed password.
-- normal users will have passwords while service accounts will have api keys.
-- API keys have a key id and key secret.
create table account_access_methods
(
    id serial primary key,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    name generic_string not null,
    hashed_password generic_string not null, --This is computed with scrypt which includes salt.
    algorithm int not null,  -- 0 is scrypt, the rest are reserved for future.
    created_at generic_timestamp,
    updated_at generic_timestamp,
    disabled_at generic_timestamp default null
);

--
create table account_organizations
(
    id serial primary key,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    organization_id integer not null references organizations(id) on update restrict on delete restrict
);


create table users
(
    id serial primary key,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    email email,
    is_disabled boolean default false,
    disabled_by integer null,
    name generic_string,
    phone_number phone_number,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);

