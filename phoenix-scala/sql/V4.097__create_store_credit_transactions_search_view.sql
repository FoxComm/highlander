drop materialized view if exists store_credit_transactions_view;

create table store_credit_transactions_search_view
(
    id bigint primary key,
    account_id integer not null,
    store_credit_id integer not null,
    created_at json_timestamp,
    store_credit_created_at json_timestamp,
    debit integer not null,
    available_balance integer not null,
    state character varying(255) not null,
    origin_type store_credit_origin_type,
    currency currency,
    order_payment jsonb not null default '{}',
    store_admin jsonb not null default '{}',
    scope exts.ltree
);
