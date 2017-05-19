create table product_reviews (
  id          serial primary key,
  scope       ltree not null,
  content     json  not null,
  user_id     int   not null references users (id) on update restrict on delete restrict,
  sku_id      int   not null references skus (id) on update restrict on delete restrict,
  updated_at  generic_timestamp,
  created_at  generic_timestamp,
  archived_at generic_timestamp
);

create table product_reviews_search_view
(
  id          integer,
  scope       exts.ltree  not null,
  sku         generic_string,
  user_name   generic_string,
  user_id     generic_string,
  title       generic_string,
  attributes  jsonb,
  created_at  generic1_string,
  updated_at  generic_string,
  archived_at generic_string
);
