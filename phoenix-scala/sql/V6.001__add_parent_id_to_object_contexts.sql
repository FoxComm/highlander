alter table object_contexts
  add column parent_id integer
    references object_contexts(id) on update restrict on delete restrict;

alter table object_contexts
  add foreign key (parent_id) references object_contexts(id);

create index object_contexts_parent_id on object_contexts (parent_id);
