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
