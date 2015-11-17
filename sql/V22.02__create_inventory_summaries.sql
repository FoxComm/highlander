create table inventory_summaries (
    id serial primary key,
    warehouse_id integer not null,
    sku_id integer not null,
    on_hand integer not null default 0,
    on_hold integer not null default 0,
    reserved integer not null default 0,
    non_sellable integer not null default 0,
    safety_stock integer not null default 0,
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (sku_id) references skus(id) on update restrict on delete restrict,
    foreign key (warehouse_id) references warehouses(id) on update restrict on delete restrict
);

create function update_inventory_summaries() returns trigger as $$
declare
    new_on_hand integer default 0;
    new_on_hold integer default 0;
    new_reserved integer default 0;
begin
    new_reserved := new.reserved;
    new_on_hand := new.on_hand;
    new_on_hold := new.on_hold;

    update inventory_summaries set on_hand=(on_hand + new_on_hand), on_hold=(on_hold + new_on_hold), reserved=(reserved + new_reserved) where warehouse_id = new.warehouse_id and sku_id = new.sku_id;
    if found then return new; end if;
    if not found then
        insert into inventory_summaries (warehouse_id, sku_id, on_hand, on_hold, reserved) values (new.warehouse_id, new.sku_id, new_on_hand, new_on_hold, new_reserved);
    end if;

    return new;
end;
$$ language plpgsql;

