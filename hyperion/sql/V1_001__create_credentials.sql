CREATE SEQUENCE amazon_credentials_id_seq;
CREATE TABLE amazon_credentials(
  id INT NOT NULL DEFAULT nextval('amazon_credentials_id_seq'),
  client_id int4 NULL,
  seller_id varchar(255) NULL,
  mws_auth_token varchar(255) NULL,
  inserted_at timestamp NOT NULL,
  updated_at timestamp NOT NULL
);

ALTER SEQUENCE amazon_credentials_id_seq OWNED BY amazon_credentials.id;