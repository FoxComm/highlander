create table cords (
    id serial primary key,
    reference_number reference_number not null unique default '',
    is_cart boolean not null default true
);

-- Generate reference number
create function set_cord_reference_number() returns trigger as $$
declare
    reference_number reference_number default 0;
    prefix character(2) default 'BR';
    start_number integer default 10000;
begin
    if length(new.reference_number) = 0 then
        new.reference_number = concat(prefix, start_number + new.id);
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_cord_reference_number_trg
    before insert
    on cords
    for each row
    execute procedure set_cord_reference_number();
