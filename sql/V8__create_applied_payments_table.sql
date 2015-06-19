-- Should this be called something more standard like 'carts_payment_methods?'
create table applied_payments (
    id serial primary key,
    order_id integer,
    payment_method_id integer,
    payment_method_type character varying(255),
    applied_amount integer,
    status character varying(255),
    response_code character varying(255)
);

alter table only applied_payments
    add constraint applied_payments_order_fk foreign key (order_id) references orders(id) on update restrict on delete restrict;

-- TODO: We should probably actually make this polymorphic.
-- alter table only applied_payments
--    add constraint applied_payments_payment_method_fk foreign key (payment_method_id) references ???

create index applied_payments_order_id_idx on applied_payments (order_id)

