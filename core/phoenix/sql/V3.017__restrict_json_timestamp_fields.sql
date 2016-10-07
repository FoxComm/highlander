alter table carts_search_view
  alter column deleted_at type json_timestamp,
  alter column created_at type json_timestamp,
  alter column updated_at type json_timestamp;

alter table gift_cards_search_view
    alter column created_at type json_timestamp,
    alter column updated_at type json_timestamp;

alter table store_admins_search_view
    alter column created_at type json_timestamp;

alter table store_credits_search_view
    alter column created_at type json_timestamp,
    alter column updated_at type json_timestamp;

alter table activity_connections_view
    alter column created_at type json_timestamp;

alter table album_search_view
    alter column archived_at type json_timestamp;

alter table sku_search_view
    alter column archived_at type json_timestamp;

alter table products_search_view
    alter column archived_at type json_timestamp,
    alter column active_from type json_timestamp,
    alter column active_to type json_timestamp;

alter table orders_search_view
    alter column placed_at type json_timestamp;

alter table customers_search_view
    alter column joined_at type json_timestamp;