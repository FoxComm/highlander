select pg_drop_replication_slot('bottledwater_phoenix') where exists (
  select * from pg_replication_slots where slot_name = 'bottledwater_phoenix' and active = false);

drop extension if exists bottledwater;
