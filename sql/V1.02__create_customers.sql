create table customers (
    id serial primary key,
    is_disabled boolean not null default false,
    disabled_by integer null,
    email email not null,
    hashed_password generic_string,
    name generic_string,
    phone_number phone_number,
    location generic_string,
    modality generic_string,
    is_guest boolean default false not null,
    is_blacklisted boolean not null default false,
    blacklisted_by integer null,
    blacklisted_reason character varying(255),
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null,
    foreign key (disabled_by) references store_admins(id) on update restrict on delete restrict
);

create index customers_email_idx on customers (email);

create unique index customers_active_non_guest_email on customers (email, is_disabled, is_guest) where
    is_disabled = false and is_guest = false;

create index customers_email_name_trgm_gin on customers using gin(lower(email) exts.gin_trgm_ops, lower(name)
exts.gin_trgm_ops);
