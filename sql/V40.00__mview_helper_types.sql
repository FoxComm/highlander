-- fake tables for materialized views casting to generate proper json keys

create table export_addresses (
    address1            text,
    address2            text,
    city                text,
    zip                 text,
    region              text,
    country             text,
    continent           text,
    currency            currency
);

create table export_assignees (
    name        text,
    assigned_at text
);

create table export_customers (
    id                  integer,
    name                text,
    email               text,
    is_blacklisted      boolean,
    joined_at           text
);

create table export_line_items (
    reference_number    text,
    state               text,
    sku                 text,
    name                text,
    price               integer
);

create table export_orders (
    customer_id         integer,
    reference_number    text,
    state               text,
    created_at          text,
    placed_at           text,
    sub_total           integer,
    shipping_total      integer,
    adjustments_total   integer,
    taxes_total         integer,
    grand_total         integer,
    items_count         integer
);

create table export_payments (
    payment_method_type text,
    amount              integer,
    currency            currency,
    credit_card_state   text,
    gift_card_state     text,
    store_credit_state  text
);

create table export_order_payments (
    order_reference_number      text,
    order_created_at            text,
    order_payment_created_at    text
);

create table export_returns (
    reference_number text,
    state            text,
    return_type         text,
    placed_at        text
);

create table export_shipments (
    state                       text,
    shipping_price              integer,
    admin_display_name          text,
    storefront_display_name     text
);

create table export_skus (
    sku     text,
    name    text,
    price   integer
);

create table export_sku_price (
    price   integer,
    currency  text
);

create table export_albums (
    name    text,
    images  jsonb
);

create table export_assignments (
    reference_number    text,
    assigned_at         text
);

create table export_store_admins (
    email       text,
    name        text,
    department  text
);

create table export_gift_cards (
    code        text,
    origin_type text,
    currency    currency,
    created_at  text
);

create table export_store_credits (
    id          integer,
    customer_id integer,
    origin_type text,
    currency    currency,
    created_at  text
);

create table export_reasons (
    reason_type text,
    body        text
);

create table export_skus_raw (
    id          integer,
    sku         text,
    attributes  jsonb,
    created_at  text
);

create table export_products_raw (
    id          integer,
    attributes  jsonb,
    created_at  text
);

create table export_promotions_raw (
    id          integer,
    apply_type  text,
    attributes  jsonb,
    created_at  text
);

create table export_coupons_raw (
    id           integer,
    promotion_id integer,
    attributes   jsonb,
    created_at   text
);

create table export_countries (
    id                  integer,
    name                text,
    alpha2              character(2),
    alpha3              character(3),
    code                character(3),
    continent           text,
    currency            currency,
    uses_postal_code    boolean,
    is_billable         boolean,
    is_shippable        boolean
);
