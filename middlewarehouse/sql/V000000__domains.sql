-- General domains
create domain generic_string text check (length(value) <= 255);
create domain generic_timestamp timestamp without time zone default (now() at time zone 'utc');

-- SKU Code
create domain sku_code text check (length(value) >= 2);

