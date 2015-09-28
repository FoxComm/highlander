create table skus (
    id serial primary key,
    sku character varying(255),
    name character varying(255),
    is_hazardous bool default false, -- This is temp before we build out items.
    price int not null -- Yax needs this for real payments.
);

