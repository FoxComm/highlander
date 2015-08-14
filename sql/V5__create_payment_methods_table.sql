create table payment_methods (
    id serial primary key
);

create function set_payment_method_id() returns trigger as $$
declare
    payment_method_id int;
begin
    insert into payment_methods default values returning id INTO payment_method_id;
    new.id = payment_method_id;
    return new;
end;
$$ language plpgsql;

