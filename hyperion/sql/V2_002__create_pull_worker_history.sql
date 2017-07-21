create table pull_worker_history(
  id bigserial primary key,
  seller_id varchar(255) not null,
  last_run timestamp not null,
  inserted_at timestamp not null,
  updated_at timestamp not null
)
