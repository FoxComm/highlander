create table tokenized_credit_cards (
    id integer not null,
    customer_id integer,
    payment_gateway character varying(255) not null,
    gateway_token_id character varying(255) not null,
    last_four_digits character varying(4),
    expiration_month integer,
    expiration_year integer,
    brand character varying(255)
);

create sequence tokenized_credit_cards_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only tokenized_credit_cards
    alter column id set default nextval('tokenized_credit_cards_id_seq'::regClass);

alter table only tokenized_credit_cards
    add constraint tokenized_credit_cards_customer_fk foreign key (customer_id) references customers(id) on update restrict on delete restrict;

alter table only tokenized_credit_cards
    add constraint tokenized_credit_cards_pkey primary key (id);
