create table line_items (
    id integer not null,
    parent_id integer not null,
    parent_type character varying(255),
    sku_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create sequence line_items_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only line_items
    add constraint line_items_pkey primary key (id);

alter table only line_items
  alter column id set default nextval('line_items_id_seq'::regclass);
