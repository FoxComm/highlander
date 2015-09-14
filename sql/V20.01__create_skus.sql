create table skus (
    id serial primary key,
    sku character varying(255),
    name character varying(255),
    price int not null -- Yax needs this for real payments.
);

