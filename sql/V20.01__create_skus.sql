create table skus (
    id integer not null,
    name character varying(255),
    price int not null -- Yax needs this for real payments.
);

create sequence skus_id_seq
     start with 1
     increment by 1
     no minvalue
     no maxvalue
     cache 1;


alter table only skus
    add constraint skus_pkey primary key (id);

alter table only skus
    alter column id set default nextval('skus_id_seq'::regclass);