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

create function update_gift_card_current_balance() returns trigger as $$
declare
    adjustment integer default 0;
begin
    if new.debit > 0 and new.capture then
        adjustment := -new.debit;
    elsif new.credit > 0 then
        adjustment := new.credit;
    end if;

    update gift_cards set current_balance = current_balance + adjustment where id = new.gift_card_id;

    return new;
end;
$$ language plpgsql;

-- ISO4217 declares currency as alphanumeric-3
create domain currency character(3) not null;

