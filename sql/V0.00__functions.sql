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
