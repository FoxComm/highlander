drop trigger if exists update_sc_txn_from_orders_payment on store_credit_adjustments;
drop function if exists update_sc_txn_view_from_order_payment_fn();

drop trigger if exists update_gc_txn_from_orders_payment on gift_card_adjustments;
drop function  if exists update_gc_txn_view_from_order_payment_fn();