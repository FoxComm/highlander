-- creates an inventory_event so we have an ID for the child table in our concrete table pattern
create function make_inventory_event_id() returns bigint as $$
declare
    event_id bigint;
begin
    insert into inventory_events default values returning id INTO event_id;
    return event_id;
end;
$$ language plpgsql;

create function set_inventory_event_id() returns trigger as $$
begin
    new.id = make_inventory_event_id();
    return new;
end;
$$ language plpgsql;

create function update_inventory_summaries() returns trigger as $$
declare
    reserved_for_fulfillment integer default 0;
begin
    reserved_for_fulfillment := new.reserved_for_fulfillment;

    update inventory_summaries set available_on_hand = (available_on_hand - reserved_for_fulfillment) where sku_id = new.sku_id;
    if found then return new; end if;
    if not found then
        insert into inventory_summaries (sku_id, available_on_hand) values (new.sku_id, -reserved_for_fulfillment);
    end if;

    return new;
end;
$$ language plpgsql;

-- ISO4217 declares currency as alphanumeric-3
create domain currency character(3) not null;

-- RFC2821 + Errata 1690 limits max email size to 254 chars
create domain email text check (length(value) <= 254);

-- Using text instead of character varying is more efficient
create domain generic_string text check (length(value) <= 255);
create domain reference_number text check (length(value) <= 20);

-- Generic phone number
create domain phone_number text check (length(value) <= 15);

-- Zip code domain
create domain zip_code text check (
    length(value) > 0 and
    length(value) <= 12 and
    value ~ '(?i)^[a-z0-9][a-z0-9\- ]{0,10}[a-z0-9]$'
);

-- RMA-specific domains used in multiple tables
create domain rma_reason_type text check (value in ('baseReason', 'productReturnCode'));
create domain rma_type text check (value in ('standard', 'creditOnly', 'restockOnly'));
create domain rma_status text check (value in ('pending', 'processing', 'review', 'complete', 'canceled'));
create domain rma_inventory_disposition text check (value in ('putaway', 'damage', 'recovery', 'discontinued'));
create domain rma_line_item_origin_type text check (value in ('skuItem', 'giftCard', 'shippingCost'));