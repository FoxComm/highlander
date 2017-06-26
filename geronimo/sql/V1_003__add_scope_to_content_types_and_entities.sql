create extension if not exists ltree with schema public;
alter table content_types add column scope ltree;
alter table content_types add column created_by int8;
alter table content_types_history add column scope ltree;
alter table content_types_history add created_by int8;

alter table entities add column scope ltree;
alter table entities add column created_by int8;
alter table entities_history add column scope ltree;
alter table entities_history add column created_by int8;