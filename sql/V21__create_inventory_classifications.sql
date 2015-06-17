create table inventory_classifications (
    id integer not null,
    sku_id integer not null,
    can_sell boolean not null,
    can_pre_order boolean not null,
    can_back_order boolean not null
);

create sequence inventory_classifications_id_seq
     start with 1
     increment by 1
     no minvalue
     no maxvalue
     cache 1;


alter table only inventory_classifications
    add constraint inventory_classifications_pkey primary key (id);

alter table only inventory_classifications
    alter column id set default nextval('inventory_classifications_id_seq'::regclass);

alter table only inventory_classifications
    add constraint inventory_classifications_sku_id_fk foreign key (sku_id) references skus(id) on update restrict on delete restrict;



