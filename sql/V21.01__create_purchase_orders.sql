-- This is a placeholder table for now.  We can blow it out later.
create purchase_orders (
    id integer not null
);

create sequence purchase_orders_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;


alter table purchase_orders
    alter column id set default nextval('purchase_orders_id_seq'::regclass);

alter table only purchase_orders
      add constraint purchase_orders_pkey primary key (id);
