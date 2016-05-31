create table coupon_usages(
    id serial primary key,
    coupon_form_id integer not null references object_forms(id) on update restrict on delete restrict,
    count integer not null default 0,
    updated_at generic_timestamp,
    created_at generic_timestamp
);

create unique index coupon_usages_coupon_form_idx on coupon_usages (coupon_form_id);

create function create_coupon_usages() returns trigger as $$
begin
    --waiting for lovely 9.5 upsert....
    if (select coupon_form_id from coupon_usages where coupon_form_id = new.form_id) then
        --done 
    else
        insert into coupon_usages (coupon_form_id) values (new.form_id); 
    end if;
    return new;
end;
$$ language plpgsql;

create trigger create_coupon_usages
    after insert on coupons for each row
    execute procedure create_coupon_usages();


