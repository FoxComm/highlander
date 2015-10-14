create table store_admins (
    id serial primary key,
    email email not null,
    hashed_password generic_string not null,
    first_name generic_string,
    last_name generic_string,
    department generic_string,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

create index store_admins_email_idx on store_admins (email)

