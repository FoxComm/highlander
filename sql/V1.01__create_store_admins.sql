create table store_admins (
    id serial primary key,
    email email not null,
    hashed_password generic_string,
    name generic_string,
    phone_number generic_string,
    department generic_string,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);

create unique index store_admins_email_idx on store_admins (email)
