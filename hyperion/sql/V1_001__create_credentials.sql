create table amazon_credentials(
  id bigserial primary key,
  client_id int4 null,
  seller_id varchar(255) null,
  mws_auth_token varchar(255) null,
  inserted_at timestamp not null,
  updated_at timestamp not null
);
