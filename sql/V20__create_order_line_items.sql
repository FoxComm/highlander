create table order_line_items (
    id integer not null,
    order_id integer not null,
    sku_id integer not null,
    status character varying(255),
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create sequence order_line_items_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only order_line_items
    add constraint order_line_items_pkey primary key (id);

alter table only order_line_items
  alter column id set default nextval('order_line_items_id_seq'::regclass);

alter table only order_line_items
    add constraint order_line_items_order_id_fk foreign key (order_id) references orders(id) on update restrict on delete restrict;