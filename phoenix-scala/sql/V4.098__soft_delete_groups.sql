alter table customer_dynamic_groups add column deleted_at timestamp;

alter table group_template_instances add column deleted_at timestamp;

alter table customer_groups_search_view add column deleted_at json_timestamp;