create table customer_profiles (
    customer_id integer primary key,
    phone_number character varying(255),
    location character varying(255),
    modality character varying(255),
    foreign key (customer_id) references customers(id) on update restrict on delete restrict
);