create table cart_line_items (
    id integer not null,
    cart_id integer not null,
    sku_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create sequence cart_line_items_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only cart_line_items
    add constraint cart_line_items_pkey primary key (id);

alter table only cart_line_items
  alter column id set default nextval('cart_line_items_id_seq'::regclass);

alter table only cart_line_items
    add constraint cart_line_items_cart_id_fk foreign key (cart_id) references carts(id) on update restrict on delete restrict;