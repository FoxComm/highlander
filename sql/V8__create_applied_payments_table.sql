-- Should this be called something more standard like 'carts_payment_methods?'
create table applied_payments (
    id integer not null,
    order_id integer,
    payment_method_id integer,
    payment_method_type character varying(255),
    applied_amount integer,
    status character varying(255),
    response_code character varying(255)
);

create sequence applied_payments_id_sequence
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;


alter table only applied_payments
    alter column id set default nextval('applied_payments_id_sequence'::regClass);

alter table only applied_payments
    add constraint applied_payments_order_fk foreign key (order_id) references orders(id) on update restrict on delete restrict;

-- TODO: We should probably actually make this polymorphic.
-- alter table only applied_payments
--    add constraint applied_payments_payment_method_fk foreign key (payment_method_id) references ???

alter table only applied_payments
    add constraint applied_payments_pkey primary key (id);
