create table credit_cards (
    id serial primary key,
    customer_id integer,
    billing_address_id integer not null,
    gateway_customer_id character varying(255) not null,
    -- gateway_id integer not null, TODO: add lookup table
    last_four character(4) not null,
    exp_month integer not null,
    exp_year integer not null,
    is_default boolean default false not null,
    constraint valid_last_four check (last_four ~ '[0-9]{4}'),
    constraint valid_exp_year check (exp_year between 2015 and 3000),
    constraint valid_exp_month check (exp_month between 1 and 12),
    foreign key (customer_id) references customers(id) on update restrict on delete restrict,
    foreign key (billing_address_id) references addresses(id) on update restrict on delete restrict
);

create index credit_cards_customer_id_idx on credit_cards (customer_id);

create unique index credit_cards_default_idx on credit_cards (customer_id, is_default);


