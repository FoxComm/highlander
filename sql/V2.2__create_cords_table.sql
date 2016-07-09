create table cords (
    id serial primary key,
    reference_number reference_number not null unique,
    cart_id integer not null,
    order_id integer,
    foreign key (cart_id) references carts(id) on update restrict on delete restrict,
    foreign key (order_id) references orders(id) on update restrict on delete restrict,
    foreign key (reference_number) references carts(reference_number) on update restrict on delete restrict
);

-- Create a cord when new cart is created
create or replace function create_cord_for_cart() returns trigger as $$
declare
begin
    insert into cords (reference_number, cart_id) values (new.reference_number, new.id);
    return new;
end;
$$ language plpgsql;

create trigger create_cord_for_cart_trg
    after insert
    on carts
    for each row
    execute procedure create_cord_for_cart();

-- Update a cord when new order is created
create or replace function update_cord_for_order() returns trigger as $$
declare
begin
    update cords set order_id = new.id where reference_number = new.reference_number;
    update carts set is_active = false where reference_number = new.reference_number;
    return new;
end;
$$ language plpgsql;

create trigger update_cord_for_order_trg
    after insert
    on orders
    for each row
    execute procedure update_cord_for_order();
