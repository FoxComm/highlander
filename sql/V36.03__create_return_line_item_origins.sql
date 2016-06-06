create table return_line_item_origins (
    id serial primary key
);

create function set_return_line_item_origin_id() returns trigger as $$
declare
    origin_id int;
begin
    insert into return_line_item_origins default values returning id INTO origin_id;
    new.id = origin_id;
    return new;
end;
$$ language plpgsql;
