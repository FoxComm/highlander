-- General domains
create domain generic_string text check (length(value) <= 255);
create domain generic_timestamp_now timestamp without time zone default (now() at time zone 'utc');
create domain generic_timestamp_null timestamp without time zone null;

-- IOS4217 declares currency as alphanumeric-3
create domain currency character(3) not null;

-- Region abbreviation
create domain region_abbr text check (length(value) <= 10);

-- Generic phone number
create domain phone_number text check (length(value) <= 15);

-- Zip code
create domain zip_code text check (
  length(value) > 0 and
  length(value) <= 12 and
  value ~ '(?i)^[a-z0-9][a-z0-9\- ]{0,10}[a-z0-9]$'
);

-- SKU Code
create domain sku_code text check (length(value) >= 2);

-- Stock Items
create domain stock_item_unit_state text not null check (value in ('onHand', 'onHold', 'reserved'));

-- Shipments
create domain shipment_state text not null check (value in ('pending', 'shipped', 'delivered', 'cancelled'));
create domain shipment_failure_reason text check (value in ('outOfStock'));

-- Stock Locations
create domain stock_location_type text not null check (value in ('Warehouse'));
