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

-- same as set_inventory_event_id() but specifically for inventory_adjustments since the FK is inventory_event_id
create function set_inventory_event_id_for_adjustments() returns trigger as $$
begin
    new.inventory_event_id = make_inventory_event_id();
    return new;
end;
$$ language plpgsql;

