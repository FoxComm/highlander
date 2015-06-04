create table cities (
    id integer not null,
    state_id integer not null,
    name character varying(255) not null,
    zip character(5) not null
);

create sequence cities_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only cities
  alter column id set default nextval('cities_id_seq'::regclass);

alter table only cities
  add constraint cities_state_id_fk foreign key (state_id) references states(id) on update restrict on delete cascade;

alter table only cities
  add constraint cities_pkey primary key (id);

insert into cities (state_id, name, zip) values
  (1, 'Burlingame', '50000'), (2, 'Seattle', '30000');
