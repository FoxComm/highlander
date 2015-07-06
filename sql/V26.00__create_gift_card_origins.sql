create table gift_card_origins (
    id serial primary key
);

create function set_gift_card_origin_id() returns trigger as $$
declare
    origin_id int;
begin
    insert into gift_card_origins default values returning id INTO origin_id;
    new.origin_id = origin_id;
    return new;
end;
$$ language plpgsql;

