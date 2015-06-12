create table orders (
    id integer not null,
    customer_id integer,
    status character varying(255),
    locked int,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

create sequence orders_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only orders
    add constraints orders_pkey primary key (id);

alter table only orders
    alter column id set default nextval('orders_id_seq'::regclass);

