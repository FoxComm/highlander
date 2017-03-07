alter domain customer_group_type drop constraint customer_group_type_check;

alter domain customer_group_type add constraint customer_group_type_check check (value in ('manual', 'dynamic', 'template'));