create domain gc_origin_type text not null check (
    value in ('customerPurchase', 'csrAppeasement', 'fromStoreCredit')
);

create table gift_card_subtypes (
    id serial primary key,
    title generic_string not null,
    origin_type gc_origin_type
);