alter table customer_dynamic_groups rename to customer_groups;

create domain customer_group_type text check (value in ('manual', 'dynamic'));

alter table customer_groups add column group_type customer_group_type not null default 'dynamic';
