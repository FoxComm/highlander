create table customers (
    id serial primary key,
    disabled boolean not null default false,
    disabled_by integer null,
    email email not null,
    hashed_password generic_string not null,
    first_name generic_string not null,
    last_name generic_string not null,
    phone_number phone_number,
    location generic_string,
    modality generic_string,
    is_guest boolean default false not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (disabled_by) references store_admins(id) on update restrict on delete restrict
);

create index customers_email_idx on customers (email);

create unique index customers_active_non_guest_email on customers (email, disabled, is_guest) where
    disabled = false and is_guest = false;

