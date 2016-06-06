create table generic_tree (
  id         serial primary key,
  name       generic_string not null,
  context_id int not null references object_contexts (id) on update restrict on delete restrict,
  unique (name, context_id)
);
create index generic_tree_name on generic_tree(name);
create index generic_tree_context_id on generic_tree(context_id);

create table generic_tree_nodes (
  id         serial primary key,
  tree_id int not null references generic_tree (id) on update restrict on delete restrict,
  index      int not null,
  path       ltree not null,
  kind       generic_string not null,
  object_id  int not null references object_forms(id) on update restrict on delete restrict,
  unique (tree_id, "index")
);

create index generic_tree_node_tree_id on generic_tree_nodes(tree_id);
create index generic_tree_node_path on generic_tree_nodes (path);
