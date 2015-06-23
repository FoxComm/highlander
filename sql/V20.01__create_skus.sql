create table skus (
    id serial primary key,
    name character varying(255),
    price int not null -- Yax needs this for real payments.
);

