alter table carts
    add constraint customer_id_fk
    foreign key (customer_id) references customers (id)
    on update restrict on delete restrict;

alter table orders
    add constraint customer_id_fk
    foreign key (customer_id) references customers (id)
    on update restrict on delete restrict;

alter table store_credits
    add constraint customer_id_fk
    foreign key (customer_id) references customers (id)
    on update restrict on delete restrict;

alter table orders
    add constraint object_context_id_fk
    foreign key (context_id) references object_contexts (id)
    on update restrict on delete restrict;
