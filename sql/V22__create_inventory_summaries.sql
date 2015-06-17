create table inventory_summaries (
    id integer not null,
    sku_id integer not null,
    available_on_hand integer not null,
    available_pre_order integer not null,
    available_back_order integer not null,
    outstanding_pre_orders integer not null,
    outstanding_back_orders integer not null
);

create sequence inventory_summaries_id_seq
     start with 1
     increment by 1
     no minvalue
     no maxvalue
     cache 1;
     
     
alter table only inventory_summaries
    add constraint inventory_summaries_pkey primary key (id);
    
alter table only inventory_summaries
    alter column id set default nextval('inventory_summaries_id_seq'::regclass);
    
alter table only inventory_summaries
    add constraint inventory_summaries_sku_id_fk foreign key (sku_id) references skus(id) on update restrict on delete restrict;