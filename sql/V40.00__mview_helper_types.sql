-- Types for materialized views casting to generate proper json keys

create type export_addresses as (
    address1            text,
    address2            text,
    city                text,
    zip                 text,
    region_name         text,
    country_name        text,
    continent           text,
    currency            currency
);

create type export_assignees as (
    first_name  text,
    last_name   text,
    assigned_at text
);

create type export_customers as (
    name                text,
    email               text,
    is_blacklisted      boolean,
    joined_at           text,
    rank                integer,
    revenue             integer
);

create type export_line_items as (
    status  text,
    sku     text,
    name    text,
    price   integer
);

create type export_orders as (
    reference_number text,
    status           text,
    created_at       text,
    placed_at        text
);

create type export_payments as (
    payment_method_type text,
    amount              integer,
    currency            currency
);

create type export_rmas as (
    reference_number text,
    status           text,
    rma_type         text,
    placed_at        text
);

create type export_shipments as (
    status                      text,
    shipping_price              integer,
    admin_display_name          text,
    storefront_display_name     text
);

create type export_skus as (
    sku     text,
    name    text,
    price   integer
);