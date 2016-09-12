alter table shipments rename reference_number to order_ref_num;
alter table shipments add column reference_number generic_string not null unique default '';

-- Generate reference number
create function set_shipment_reference_number() returns trigger as $$
declare
    reference_number generic_string default 0;
    prefix character(2) default 'FS';
    start_number integer default 10000;
begin
    if length(new.reference_number) = 0 then
        new.reference_number = concat(prefix, start_number + new.id);
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_shipment_reference_number_trg
    before insert
    on shipments
    for each row
    execute procedure set_shipment_reference_number();
