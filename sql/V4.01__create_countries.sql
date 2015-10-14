create table countries (
    id serial primary key,
    name generic_string not null,
    alpha2 character(2) not null,
    alpha3 character(3) not null,
    code character(3) null,
    continent generic_string not null,
    currency currency not null,
    languages character(2)[],
    uses_postal_code boolean default false not null,
    is_shippable boolean default false not null,
    is_billable boolean default false not null,
    constraint valid_alpha2 check (alpha2 ~ '[a-zA-Z]{2}'),
    constraint valid_alpha3 check (alpha3 ~ '[a-zA-Z]{3}'),
    constraint valid_code check (code ~ '[0-9]{3}')
);

create index countries_code_idx on countries (code);
create index countries_alpha2_idx on countries (alpha2);
create index countries_alpha3_idx on countries (alpha3);

