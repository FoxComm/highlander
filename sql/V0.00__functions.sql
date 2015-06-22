-- creates an inventory_event so we have an ID for the child table in our concrete table pattern
create function set_inventory_event_id() returns trigger as $$
declare
    event_id bigint;
begin
    insert into inventory_events default values returning id INTO event_id;
    new.id = event_id();
    return new;
end;
$$ language plpgsql;

