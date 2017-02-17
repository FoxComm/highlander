create or replace function set_return_line_item_origin_id() returns trigger as $$
declare
    origin_id int;
begin
    insert into return_line_item_origins default values returning id into origin_id;
    new.id = origin_id;
    return new;
end;
$$ language plpgsql;

drop trigger if exists set_return_line_item_sku_id on return_line_item_skus;
create trigger set_return_line_item_sku_id
    before insert
    on return_line_item_skus
    for each row
    execute procedure set_return_line_item_origin_id();

drop trigger if exists set_return_line_item_shipment_id on return_line_item_shipments;
create trigger set_return_line_item_shipment_id
    before insert
    on return_line_item_shipments
    for each row
    execute procedure set_return_line_item_origin_id();

create or replace function set_rli_refnum() returns trigger as $$
begin
    if length(new.reference_number) = 0 then
        new.reference_number = md5(random()::text || clock_timestamp()::text)::uuid::text;
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists set_rli_refnum_trg on return_line_items;
create trigger set_rli_refnum_trg
    before insert
    on return_line_items
    for each row
    execute procedure set_rli_refnum();
