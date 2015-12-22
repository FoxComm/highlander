-- fake tables for materialized views casting to generate proper json keys

create table export_addresses (
    address1            text,
    address2            text,
    city                text,
    zip                 text,
    region              text,
    country             text,
    continent           text,
    currency            character(3)
);

create table export_assignees (
    name        text,
    assigned_at text
);

create table export_customers (
    name                text,
    email               text,
    is_blacklisted      boolean,
    joined_at           text,
    rank                integer,
    revenue             integer
);

create table export_line_items (
    status  text,
    sku     text,
    name    text,
    price   integer
);

create table export_orders (
    reference_number    text,
    status              text,
    created_at          text,
    placed_at           text,
    sub_total           integer,
    shipping_total      integer,
    adjustments_total   integer,
    taxes_total         integer,
    grand_total         integer
);

create table export_payments (
    payment_method_type text,
    amount              integer,
    currency            character(3)
);

create table export_rmas (
    reference_number text,
    status           text,
    rma_type         text,
    placed_at        text
);

create table export_shipments (
    status                      text,
    shipping_price              integer,
    admin_display_name          text,
    storefront_display_name     text
);

create table export_skus (
    sku     text,
    name    text,
    price   integer
);

create table export_assignments (
  reference_number    text,
  assigned_at         text
);
