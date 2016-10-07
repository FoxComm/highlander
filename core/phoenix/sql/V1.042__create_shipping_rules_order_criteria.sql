create table shipping_price_rules_order_criteria(
    id serial primary key,
    shipping_price_rule_id integer not null,
    order_criterion_id integer not null,
    foreign key (shipping_price_rule_id) references shipping_price_rules(id) on update restrict on delete cascade,
    foreign key (order_criterion_id) references order_criteria(id) on update restrict on delete cascade
);