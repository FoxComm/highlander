create function delete_cart_line_items() returns trigger as $$
begin
  delete from cart_line_item_skus where cord_ref = old.reference_number;
  return null;
end;
$$ language plpgsql;


create trigger delete_line_items_on_cart_delete
after delete on carts
for each row execute procedure delete_cart_line_items();
