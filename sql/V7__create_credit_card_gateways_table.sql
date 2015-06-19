create table credit_card_gateways (
    id serial primary key,
    customer_id integer,
    gateway_customer_id character varying(255) not null,
    -- gateway_id integer not null, TODO: add lookup table
    last_four character(4),
    exp_month integer,
    exp_year integer
);

alter table only credit_card_gateways
    add constraint credit_card_gateways_customer_fk foreign key (customer_id) references customers(id) on update restrict on delete restrict;

