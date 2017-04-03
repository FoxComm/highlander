alter table amazon_submission_results
  add column product_feed_result jsonb,
  add column price_feed_result jsonb,
  add column inventory_feed_result jsonb,
  add column variations_feed_result jsonb,
  add column images_feed_result jsonb,
  add column completed boolean default false;