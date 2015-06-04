create table addresses (
    id integer not null,
    account_id integer not null, -- TODO: how do we handle guest addresses?
    city_id integer not null, -- TODO: nullable for foreign addresses?
    name character varying(255) not null, -- TODO: probably need > 255 chars?
    street1 character varying(255) not null, -- TODO: can we have no street at all?
    street2 character varying(255) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

create sequence addresses_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only addresses
  alter column id set default nextval('addresses_id_seq'::regclass);

alter table only addresses
  add constraint city_id foreign key (city_id) references cities(id) on update restrict on delete cascade;
