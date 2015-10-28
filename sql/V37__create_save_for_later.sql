create table save_for_later (
    id serial primary key,
    customer_id integer not null references customers(id) on update restrict on delete restrict,
    sku_id integer not null references skus(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create unique index save_for_later_customer_sku on save_for_later (customer_id, sku_id);