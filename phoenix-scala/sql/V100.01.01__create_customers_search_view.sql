create table customers_search_view
(
    -- Customer
    id bigint not null unique,
    name generic_string,
    email email not null,
    is_disabled boolean not null default false,
    is_guest boolean default false not null,
    is_blacklisted boolean not null default false,
    phone_number phone_number,
    location generic_string,
    blacklisted_by integer null,
    blacklisted_reason generic_string,
    joined_at text,
    -- Orders
    order_count integer not null default 0,
    orders jsonb not null default '[]',
    -- Shipping addresses
    shipping_addresses_count integer not null default 0,
    shipping_addresses jsonb not null default '[]',
    -- Billing addresses
    billing_addresses_count integer not null default 0,
    billing_addresses jsonb not null default '[]',
    -- Store Credits
    store_credit_count integer not null default 0,
    store_credit_total integer not null default 0
);
