-- insert amazon order function
create or replace function update_amazon_orders_view_insert_fn() returns trigger as $$
  begin
    insert into amazon_orders_search_view (
      id,
      amazon_order_id,
      order_total,
      payment_method_detail,
      order_type,
      currency,
      order_status,
      purchase_date,
      updated_at,
      created_at
    ) select distinct on (new.id)
        new.id as id,
        new.amazon_order_id as amazon_order_id,
        new.order_total as order_total,
        new.payment_method_detail as payment_method_detail,
        new.order_type as order_type,
        new.currency as currency,
        new.order_status as order_status,
        to_char(new.purchase_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as purchase_date,
        to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
        to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at;
    return null;
  end;
$$ language plpgsql;


-- delete amazon order function
drop function if exists update_amazon_orders_view_delete_fn();

-- recreate insert amazon order trigger
drop trigger if exists update_amazon_orders_view_insert_trigger on amazon_orders;
create trigger update_amazon_orders_view_insert_trigger
  after insert on amazon_orders
  for each row
  execute procedure update_amazon_orders_view_insert_fn();

-- recreate delete amazon order trigger
drop trigger if exists update_amazon_orders_view_delete_trigger on amazon_orders;
