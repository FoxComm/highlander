create table credit_card_gateways (
    id serial primary key,
    customer_id integer,
    gateway_customer_id character varying(255) not null,
    -- gateway_id integer not null, TODO: add lookup table
    last_four character(4) not null,
    exp_month integer not null,
    exp_year integer not null,
    constraint valid_last_four check (last_four ~ '[0-9]{4}'),
    constraint valid_exp_year check (exp_year between 2015 and 3000),
    constraint valid_exp_month check (exp_month between 1 and 12)
);

alter table only credit_card_gateways
    add constraint credit_card_gateways_customer_fk foreign key (customer_id) references customers(id) on update restrict on delete restrict;

create index credit_card_gateways_customer_id_idx on credit_card_gateways (customer_id)

