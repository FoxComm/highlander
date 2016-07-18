create table stock_item_summaries (
  id serial primary key,
  stock_item_id integer not null references stock_items(id) on update restrict on delete restrict,
  on_hand integer not null,
  on_hold integer not null,
  reserved integer not null,

  created_at generic_timestamp,
  updated_at generic_timestamp,
  deleted_at timestamp without time zone null,
  foreign key (stock_item_id) references stock_items(id) on update restrict on delete restrict
);

create index stock_item_summary_idx on stock_item_summaries (stock_item_id);
