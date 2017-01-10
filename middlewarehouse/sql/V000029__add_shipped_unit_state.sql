alter domain stock_item_unit_state drop constraint stock_item_unit_state_check;
alter domain stock_item_unit_state add constraint stock_item_unit_state_check check (value in ('onHand', 'onHold', 'reserved', 'shipped'));
