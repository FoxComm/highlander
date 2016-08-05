create domain transaction_type text not null check (value in ('creditCard', 'giftCard', 'storeCredit'));

alter table shipment_transactions add column type transaction_type