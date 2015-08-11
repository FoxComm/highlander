create table credit_card_charges (
    id serial primary key,
    credit_card_id integer not null,
    gateway_charge_id character varying(255) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (credit_card_id) references credit_cards(id) on update restrict on delete restrict
);

