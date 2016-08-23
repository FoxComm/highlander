create table cart_line_item_skus (
    id serial primary key,
    reference_number generic_string not null unique,
    cord_ref text not null references cords(reference_number) on update restrict on delete restrict,
    sku_id int not null REFERENCES skus(id),
    created_at generic_timestamp
);

create function set_cli_refnum() returns trigger as $$
begin
    if length(new.reference_number) = 0 then
        new.reference_number = md5(random()::text || clock_timestamp()::text)::uuid::text;
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_cli_refnum_trg
before insert on cart_line_item_skus
for each row execute procedure set_cli_refnum();

insert into cart_line_item_skus (reference_number, cord_ref, sku_id, created_at)
  select oli.reference_number, oli.cord_ref, olis.sku_id, oli.created_at
   from order_line_items as oli
     inner join carts on carts.reference_number = oli.cord_ref
     inner join order_line_item_skus as olis on oli.origin_type = 'skuItem' and olis.id = oli.origin_id;

delete from order_line_items where cord_ref in (select reference_number from carts)
