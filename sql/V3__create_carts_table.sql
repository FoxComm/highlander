create table carts (
    id integer not null,
    customer_id integer,
    status character varying(255),
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create sequence carts_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only carts
    add constraint carts_pkey primary key (id);

alter table only carts
  alter column id set default nextval('carts_id_seq'::regclass);
