alter table customers_search_view add column groups jsonb not null default '[]';

create unique index group_and_customer_data_id_unique_idx on customer_group_members (group_id, customer_data_id);