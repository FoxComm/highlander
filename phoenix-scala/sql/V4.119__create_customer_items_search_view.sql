drop materialized view if exists customer_items_view;

create table customer_items_view (
  id bigint primary key,
  scope exts.ltree not null,
  customer_id integer not null,
  customer_name generic_string not null,
  customer_email generic_string not null,
  sku_code generic_string not null,
  sku_title generic_string not null,
  sku_price text not null,
  order_reference_number reference_number not null,
  order_placed_at text,
  line_item_state text,
  saved_for_later_at text
);
