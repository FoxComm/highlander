alter table products_catalog_view add primary key (id);
alter table product_sku_links_view add primary key (product_id);
alter table coupon_codes_search_view add primary key (id);
alter table gift_cards_search_view add primary key (id);
alter table store_admins_search_view add primary key (id);
alter table activity_connections_view add primary key (id);
alter table album_search_view add primary key (album_id, context_id);
alter table promotions_search_view add primary key (id, context);
alter table promotion_discount_links_view add primary key (promotion_id);
alter table product_album_links_view add primary key (product_id);
alter table customers_search_view add primary key (id);

alter table products_search_view add constraint products_search_view_id unique (id);
alter table products_search_view add primary key (id);

alter table sku_search_view add constraint sku_search_view_id unique (id);
alter table sku_search_view add primary key (id);

alter table store_credits_search_view add primary key (id);
alter table notes_search_view add primary key (id);

alter table orders_search_view add primary key (id);
alter table carts_search_view add primary key (id);
alter table coupons_search_view add primary key (id);
