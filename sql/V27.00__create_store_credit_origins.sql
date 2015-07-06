create table store_credit_origins (
    id serial primary key
);

create function set_store_credit_origin_id() returns trigger as $$
declare
    origin_id int;
begin
    insert into store_credit_origins default values returning id INTO origin_id;
    new.origin_id = origin_id;
    return new;
end;
$$ language plpgsql;

