--drop table taxons;
--drop table taxon_terms;
--drop table taxon_term_links;
create table taxons(
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

create table taxon_terms (
  id serial primary key,
  context_id integer not null references object_contexts(id) on update restrict on delete restrict,
  shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
  form_id integer not null references object_forms(id) on update restrict on delete restrict,
  commit_id integer references object_commits(id) on update restrict on delete restrict,
  updated_at generic_timestamp,
  created_at generic_timestamp,
  archived_at generic_timestamp
);

create table taxon_term_links (
  id serial PRIMARY KEY,
  index integer not null,
  taxon_id integer not null references taxons(id),
  taxon_term_id integer not null references taxon_terms(id),
  position integer not null,
  path ltree  not null,
  updated_at generic_timestamp,
  created_at generic_timestamp,
  archived_at generic_timestamp
);

create index taxon_term_link_taxon_idx on taxon_term_links (taxon_id);
create index taxon_term_link_term_idx on taxon_term_links (taxon_term_id);

create table product_taxon_links (
  id serial primary key,
  left_id  integer not null references products(id) on update restrict on delete restrict,
  right_id integer not null references taxon_terms(id) on update restrict on delete restrict,
  created_at generic_timestamp,
  updated_at generic_timestamp,
  archived_at generic_timestamp
);

create index product_taxon_link_left_idx on product_taxon_links (left_id);
create index product_taxon_link_right_idx on product_taxon_links (right_id);


create table taxon_search_view (
  id int not null,
  name generic_string not null,
  type generic_string not null,
  values_count int not null,
  created_at generic_timestamp,
  updated_at generic_timestamp,
  active_from generic_timestamp,
  active_to generic_timestamp
)
