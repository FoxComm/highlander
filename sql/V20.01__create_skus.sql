create table skus (
    id serial primary key,
    code generic_string,
    name generic_string,
    is_active bool not null default true,
    is_hazardous bool not null default false, -- This is temp before we build out items.
    price int not null -- Yax needs this for real payments.
);

create function create_order_line_item_skus_mapping() returns trigger as $$
begin
    insert into order_line_item_skus (sku_id) values (new.id);
    return new;
end;
$$ language plpgsql;

create trigger create_order_line_item_skus_mapping
    after insert
    on skus
    for each row
    execute procedure create_order_line_item_skus_mapping();

