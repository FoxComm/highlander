create table rma_receipts (
    id bigint primary key,
    rma_id int not null,
    receiver_name character varying(255), -- Placeholder
    inventory_location_id int, -- Placeholder
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    foreign key (rma_id) references rmas(id) on update restrict on delete restrict
);