create table admin_users (
    id integer not null,
    email character varying(255) not null,
    hashed_password character varying(255) not null,
    first_name character varying(255),
    last_name character varying(255),
    department character varying(255),
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

create sequence admin_users_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only admin_users
    alter column id set default nextval('admin_users_id_seq'::regclass);

alter table only admin_users
    add constraint admin_users_pkey primary key (id);
