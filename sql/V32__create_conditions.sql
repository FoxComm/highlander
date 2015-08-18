create table conditions (
    id serial primary key,
    subject character varying(255) not null,
    field character varying(255) not null,
    operator character varying(255) not null,
    valInt integer null,
    valString character varying(255) null
);
