create or replace function update_returns_search_view_from_returns_insert_fn() returns trigger as $$
   begin
           insert into returns_search_view (
               id,
               reference_number,
               order_id,
               order_ref,
               state,
               message_to_account,
               return_type,
               customer
               )
           select distinct on (new.id)
               new.id as id,
               new.reference_number as reference_number,
               new.order_id as order_id,
               new.order_ref as order_ref,
               new.state as state,
               new.message_to_account as message_to_account,
               new.return_type as return_type,
               -- customer
               json_build_object(
                   'id', c.id,
                   'name', c.name,
                   'email', c.email,
                   'is_blacklisted', c.is_blacklisted,
                   'joined_at', c.joined_at,
                   'rank', c.rank,
                   'revenue', c.revenue
               )::jsonb as customer
           from customers_search_view as c
           where (new.account_id = c.id);
           return null;
       end;
$$ language plpgsql;

create or replace function update_returns_view_from_returns_fn() returns trigger as $$
begin
  update returns_search_view set
    state = new.state,
    return_type = new.return_type,
    message_to_account = new.message_to_account
    -- TODO update more fields?
  where id = new.id;
  return null;
end;
$$ language plpgsql;

drop trigger if exists update_returns_search_view_from_returns_insert on returns;

create trigger update_returns_search_view_from_returns_insert
after insert on returns
for each row
execute procedure update_returns_search_view_from_returns_insert_fn();

drop trigger if exists update_returns_view_from_returns on returns;

create trigger update_returns_view_from_returns
after update on returns
for each row
execute procedure update_returns_view_from_returns_fn();