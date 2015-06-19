create table orders (
    id bigint primary key,
    customer_id integer,
    status character varying(255) not null,
    locked boolean default false,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    constraint valid_status check (status in ('cart','ordered','fraudhold','remorsehold','manualhold','canceled',
                                              'fulfillmentstarted','partiallyshipped','shipped'))
);

create index orders_customer_and_status_idx on orders (customer_id, status)

