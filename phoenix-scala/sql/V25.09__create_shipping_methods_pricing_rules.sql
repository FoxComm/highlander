create table shipping_methods_price_rules(
    id serial primary key,
    shipping_method_id integer not null,
    shipping_price_rule_id integer not null,
    rule_rank integer not null,
    foreign key (shipping_method_id) references shipping_methods(id) on update restrict on delete cascade,
    foreign key (shipping_price_rule_id) references shipping_price_rules(id) on update restrict on delete cascade
);