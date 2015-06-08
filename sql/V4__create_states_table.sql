create table states (
    id integer not null,
    name character varying(255) not null,
    abbreviation character(2) not null
);

create sequence states_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only states
  alter column id set default nextval('states_id_seq'::regclass);

alter table only states
  add constraint states_pkey primary key (id);
