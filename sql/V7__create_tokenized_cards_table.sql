create table tokenized_credit_cards (
    id integer not null,
    account_id integer,
    payment_gateway character varying(255) not null,
    gateway_token_id character varying(255) not null,
    last_four_digits character varying(4),
    expiration_month integer,
    expiration_year integer,
    brand character varying(255)
);

create sequence tokenized_credit_cards_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

alter table only tokenized_credit_cards
    alter column id set default nextval('tokenized_credit_cards_id_seq'::regClass);

alter table only tokenized_credit_cards
    add constraint tokenized_credit_cards_account_fk foreign key (account_id) references accounts(id) on update restrict on delete restrict;

alter table only tokenized_credit_cards
    add constraint tokenized_credit_cards_pkey primary key (id);
