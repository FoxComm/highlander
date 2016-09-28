create table taxonomies(
  id serial primary key,
  hierarchical boolean not null,
  context_id integer not null references object_contexts(id) on update restrict on delete restrict,
  shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
  form_id integer not null references object_forms(id) on update restrict on delete restrict,
  commit_id integer references object_commits(id) on update restrict on delete restrict,
  updated_at generic_timestamp,
  created_at generic_timestamp,
  archived_at generic_timestamp
);

create table taxons (
  id serial primary key,
  context_id integer not null references object_contexts(id) on update restrict on delete restrict,
  shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
  form_id integer not null references object_forms(id) on update restrict on delete restrict,
  commit_id integer references object_commits(id) on update restrict on delete restrict,
  updated_at generic_timestamp,
  created_at generic_timestamp,
  archived_at generic_timestamp
);

create table taxonomy_taxon_links (
  id serial PRIMARY KEY,
  index integer not null,
  taxonomy_id integer not null references taxonomies(id),
  taxon_id integer not null references taxons(id),
  position integer not null,
  path ltree  not null,
  updated_at generic_timestamp,
  created_at generic_timestamp,
  archived_at generic_timestamp
);

create index taxonomy_taxon_link_taxonomy_idx on taxonomy_taxon_links (taxonomy_id);
create index taxonomy_taxon_link_term_idx on taxonomy_taxon_links (taxon_id);

create table product_taxonomy_links (
  id serial primary key,
  left_id  integer not null references products(id) on update restrict on delete restrict,
  right_id integer not null references taxons(id) on update restrict on delete restrict,
  created_at generic_timestamp,
  updated_at generic_timestamp,
  archived_at generic_timestamp
);

create index product_taxonomy_link_left_idx on product_taxonomy_links (left_id);
create index product_taxonomy_link_right_idx on product_taxonomy_links (right_id);


create table taxonomy_search_view (
  id int not null,
  name generic_string not null,
  type generic_string not null,
  values_count int not null,
  created_at generic_timestamp,
  updated_at generic_timestamp,
  active_from generic_timestamp,
  active_to generic_timestamp
)
