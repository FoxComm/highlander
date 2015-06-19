create table addresses (
    id serial primary key,
    customer_id integer not null, -- TODO: how do we handle guest addresses?
    state_id integer not null, -- TODO: nullable for foreign addresses?
    name character varying(255) not null, -- TODO: probably need > 255 chars?
    street1 character varying(255) not null, -- TODO: can we have no street at all?
    street2 character varying(255) null,
    city character varying(255) not null, -- TODO: nullable for foreign addresses?
    zip character (5) not null, -- TODO: nullable for foreign addresses?
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

alter table only addresses
  add constraint addresses_state_id_fk foreign key (state_id) references states(id) on update restrict on delete cascade,
  add constraint addresses_customer_id_fk foreign key (customer_id) references customers(id) on update restrict on delete cascade;

