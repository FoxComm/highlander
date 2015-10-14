create table skus (
    id serial primary key,
    sku generic_string,
    name generic_string,
    is_hazardous bool not null default false, -- This is temp before we build out items.
    price int not null -- Yax needs this for real payments.
);

