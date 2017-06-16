alter table skus add column unit_cost_currency generic_string not null;
alter table skus rename column unit_cost to unit_cost_value;
