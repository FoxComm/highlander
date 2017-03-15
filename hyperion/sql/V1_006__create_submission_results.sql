create table amazon_submission_results(
  id bigserial primary key,
  product_id int4 not null,
  product_feed jsonb null,
  price_feed jsonb null,
  inventory_feed jsonb null,
  images_feed jsonb null,
  inserted_at timestamp not null,
  updated_at timestamp not null
);
