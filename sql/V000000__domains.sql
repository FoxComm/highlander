-- General domains
create domain generic_string text check (length(value) <= 255);
create domain generic_timestamp_now timestamp without time zone default (now() at time zone 'utc');
create domain generic_timestamp_null timestamp without time zone null;

-- SKU Code
create domain sku_code text check (length(value) >= 2);

-- Stock Items
create domain stock_item_unit_state text not null check (value in ('onHand', 'onHold', 'reserved'));

--Shipments
create domain shipment_state text check (value in ('pending', 'shipped', 'delivered', 'cancelled'));
create domain shipment_failure_reason text check (value in ('outOfStock'));
