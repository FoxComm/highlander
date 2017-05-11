create table product_reviews(
  id serial primary key,
  scope ltree not null,
  context_id integer not null references object_contexts(id) on update restrict on delete restrict,
  shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
  form_id integer not null references object_forms(id) on update restrict on delete restrict,
  commit_id integer references object_commits(id) on update restrict on delete restrict,
  user_id int not null references users(id) on update restrict on delete restrict,
  sku_id int not null references skus(id) on update restrict on delete restrict,
  updated_at generic_timestamp,
  created_at generic_timestamp,
  archived_at generic_timestamp
);

create index product_reviews_object_context_idx on product_reviews (context_id);
create index product_reviews_review_comment_form_idx on product_reviews (form_id);
