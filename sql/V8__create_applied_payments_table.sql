-- Should this be called something more standard like 'carts_payment_methods?'
create table applied_payments (
    id integer not null,
    cart_id integer,
    payment_method_id integer,
    payment_method_type character varying(255),
    applied_amount float,
    status character varying(255),
    response_code character varying(255)
);

create sequence applied_payments_id_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


alter table only applied_payments
    alter column id set default nextval('applied_payments_id_sequence'::regClass);

alter table only applied_payments
    add constraint applied_payments_cart_fk foreign key (cart_id) references carts(id) on update restrict on delete restrict;

-- TODO: We should probably actually make this polymorphic.
-- alter table only applied_payments
--    add constraint applied_payments_payment_method_fk foreign key (payment_method_id) references ???

alter table only applied_payments
    add constraint applied_payments_pkey primary key (id);