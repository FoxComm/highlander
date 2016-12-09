update stock_locations set scope = '1.2' where scope is null;

alter table stock_locations alter column scope set not null;
