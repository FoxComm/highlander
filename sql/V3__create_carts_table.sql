CREATE TABLE carts (
    id integer NOT NULL,
    customer_id integer,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

CREATE SEQUENCE carts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE ONLY carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY carts
  ALTER COLUMN id SET DEFAULT nextval('carts_id_seq'::regclass);
