CREATE TABLE public.amazon_credentials (
  id int4 NOT NULL DEFAULT nextval('amazon_credentials_id_seq'::regclass),
  client_id int4 NULL,
  seller_id varchar(255) NULL,
  mws_auth_token varchar(255) NULL,
  inserted_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  CONSTRAINT amazon_credentials_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
) ;
