select pg_drop_replication_slot('bottledwater') where exists (
  select * from pg_replication_slots where slot_name = 'bottledwater');

drop extension if exists bottledwater;
