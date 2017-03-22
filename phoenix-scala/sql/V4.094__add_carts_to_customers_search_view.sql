alter table customers_search_view add column carts jsonb not null default '[]';

create table export_carts (
    customer_id         integer,
    reference_number    text,
    created_at          json_timestamp,
    updated_at          json_timestamp,
    sub_total           integer,
    shipping_total      integer,
    adjustments_total   integer,
    taxes_total         integer,
    grand_total         integer,
    items_count         integer
);
