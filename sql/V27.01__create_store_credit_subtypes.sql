create domain sc_origin_type text not null check (
    value in ('giftCardTransfer', 'csrAppeasement', 'returnProcess')
);

create table store_credit_subtypes (
    id serial primary key,
    title generic_string not null,
    origin_type sc_origin_type
);