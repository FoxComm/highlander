alter table gift_card_transactions_view add column scope exts.ltree;
alter table gift_card_transactions_view drop column gift_card_created_at;
