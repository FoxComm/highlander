CREATE TABLE line_items (
    id integer NOT NULL,
    cart_id integer NOT NULL,
    sku_id integer NOT NULL,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

CREATE SEQUENCE line_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE ONLY line_items
    ADD CONSTRAINT line_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY line_items
  ALTER COLUMN id SET DEFAULT nextval('line_items_id_seq'::regclass);
