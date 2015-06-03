CREATE TABLE accounts (
    id integer NOT NULL,
    created_at timestamp without time zone default (now() at time zone 'utc')
    updated_at timestamp without time zone null
    deleted_at timestamp without time zone null
    email character varying(255) NOT NULL,
    hashed_password character varying(255),
    first_name character varying(255),
    last_name character varying(255)
);

CREATE SEQUENCE accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE ONLY accounts
  ALTER COLUMN id SET DEFAULT nextval('accounts_id_seq'::regclass);
