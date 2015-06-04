create table accounts (
    id integer not null,
    email character varying(255) not null,
    plaintext_password character varying(255),
    hashed_password character varying(255) not null,
    first_name character varying(255),
    last_name character varying(255),
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

create sequence accounts_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only accounts
  alter column id set default nextval('accounts_id_seq'::regclass);

alter table only accounts
  add constraint accounts_pkey primary key (id);
