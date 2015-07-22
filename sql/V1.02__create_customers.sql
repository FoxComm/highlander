create table customers (
    id serial primary key,
    disabled boolean not null default false,
    disabled_by integer null,
    email email not null,
    hashed_password character varying(255) not null,
    first_name character varying(255),
    last_name character varying(255),
    phone_number character varying(12),
    location character varying(255),
    modality character varying(255),
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (disabled_by) references store_admins(id) on update restrict on delete restrict
);

create index customers_email_idx on customers (email)

