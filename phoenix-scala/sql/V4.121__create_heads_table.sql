create table heads (
       id serial primary key,
       kind generic_string not null,
       view_id integer not null references object_contexts(id) on update restrict on delete restrict,
       commit_id integer not null references object_commits(id) on update restrict on delete restrict,
       created_at generic_timestamp,
       updated_at generic_timestamp,
       archived_at generic_timestamp
);

create index heads_kind_view_idx on heads (kind, view_id);
