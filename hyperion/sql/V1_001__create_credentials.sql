CREATE TABLE amazon_credentials(
  id bigserial primary key,
  client_id int4 NULL,
  seller_id varchar(255) NULL,
  mws_auth_token varchar(255) NULL,
  inserted_at timestamp NOT NULL,
  updated_at timestamp NOT NULL
);
