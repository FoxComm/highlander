create table inventory_events (
    id integer not null
);

create sequence inventory_events_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;


alter table only inventory_events
    add constraint inventory_events_pkey primary key (id);


alter table only inventory_events
    alter column id set default nextval('inventory_events_id_seq'::regclass);

