
// list of all available types https://github.com/FoxComm/phoenix-scala/blob/master/app/services/activity/Tailored.scala

const types = {
  /* Assignments */

  ASSIGNED_TO_ORDER: 'assigned_to_order',
  UNASSIGNED_FROM_ORDER: 'unassigned_from_order',
  BULK_ASSIGNED_TO_ORDERS: 'bulk_assigned_to_orders',
  BULK_UNASSIGNED_FROM_ORDERS: 'bulk_unassigned_from_orders',

  /* Watchers */

  ADDED_WATCHERS_TO_ORDER: 'added_watchers_to_order',
  REMOVED_WATCHER_FROM_ORDER: 'removed_watcher_from_order',
  BULK_ADDED_WATCHER_TO_ORDERS: 'bulk_added_watcher_to_orders',
  BULK_REMOVED_WATCHER_FROM_ORDERS: 'bulk_removed_watcher_from_orders',

  /* Customers */

  CUSTOMER_CREATED: 'customer_created',
  CUSTOMER_REGISTERED: 'customer_registered',
  CUSTOMER_ACTIVATED: 'customer_activated',
  CUSTOMER_BLACKLISTED: 'customer_blacklisted',
  CUSTOMER_REMOVED_FROM_BLACKLIST: 'customer_removed_from_blacklist',
  CUSTOMER_ENABLED: 'customer_enabled',
  CUSTOMER_DISABLED: 'customer_disabled',
  CUSTOMER_UPDATED: 'customer_updated',

  /* Customer Addresses */

  CUSTOMER_ADDRESS_CREATED_BY_ADMIN: 'customer_address_created_by_admin',
  CUSTOMER_ADDRESS_CREATED: 'customer_address_created',
  CUSTOMER_ADDRESS_UPDATED: 'customer_address_updated',
  CUSTOMER_ADDRESS_DELETED: 'customer_address_deleted',

  /* Customer Credit Cards */

  CREDIT_CARD_ADDED: 'credit_card_added',
  CREDIT_CARD_UPDATED: 'credit_card_updated',
  CREDIT_CARD_REMOVED: 'credit_card_removed',

  /* Orders */

  CART_CREATED: 'cart_created',
  ORDER_STATE_CHANGED: 'order_state_changed',
  ORDER_BULK_STATE_CHANGED: 'order_bulk_state_changed',

  /* Order Line Items */

  ORDER_LINE_ITEMS_ADDED_GIFT_CARD: 'order_line_items_added_gift_card',
  ORDER_LINE_ITEMS_UPDATED_GIFT_CARD: 'order_line_items_updated_gift_card',
  ORDER_LINE_ITEMS_DELETED_GIFT_CARD: 'order_line_items_deleted_gift_card',
  ORDER_LINE_ITEMS_UPDATED_QUANTITIES: 'order_line_items_updated_quantities',
  ORDER_LINE_ITEMS_UPDATED_QUANTITIES_BY_CUSTOMER: 'order_line_items_updated_quantities_by_customer',

  /* Order Shipping Methods */

  ORDER_SHIPPING_METHOD_UPDATED: 'order_shipping_method_updated',
  ORDER_SHIPPING_METHOD_REMOVED: 'order_shipping_method_removed',

  /* Order Shipping Addresses */

  ORDER_SHIPPING_ADDRESS_ADDED: 'order_shipping_address_added',
  ORDER_SHIPPING_ADDRESS_UPDATED: 'order_shipping_address_updated',
  ORDER_SHIPPING_ADDRESS_REMOVED: 'order_shipping_address_removed',

  /* Order Payment Methods */

  ORDER_PAYMENT_METHOD_ADDED_CREDIT_CARD: 'order_payment_method_added_credit_card',
  ORDER_PAYMENT_METHOD_ADDED_GIFT_CARD: 'order_payment_method_added_gift_card',
  ORDER_PAYMENT_METHOD_ADDED_STORE_CREDIT: 'order_payment_method_added_store_credit',
  ORDER_PAYMENT_METHOD_DELETED: 'order_payment_method_deleted',
  ORDER_PAYMENT_METHOD_DELETED_GIFT_CARD: 'order_payment_method_deleted_gift_card',

  /* Order Notes */

  ORDER_NOTE_CREATED: 'order_note_created',
  ORDER_NOTE_UPDATED: 'order_note_updated',
  ORDER_NOTE_DELETED: 'order_note_deleted',

  /* Gift Cards */

  GIFT_CARD_CREATED: 'gift_card_created',
  GIFT_CARD_STATE_CHANGED: 'gift_card_state_changed',
  GIFT_CARD_CONVERTED_TO_STORE_CREDIT: 'gift_card_converted_to_store_credit',
  GIFT_CARD_AUTHORIZED_FUNDS: 'gift_card_authorized_funds',
  GIFT_CARD_CAPTURED_FUNDS: 'gift_card_captured_funds',

  /* Store Credits */

  STORE_CREDIT_CREATED: 'store_credit_created',
  STORE_CREDIT_STATE_CHANGED: 'store_credit_state_changed',
  STORE_CREDIT_CONVERTED_TO_GIFT_CARD: 'store_credit_converted_to_gift_card',
  STORE_CREDIT_AUTHORIZED_FUNDS: 'store_credit_authorized_funds',
  STORE_CREDIT_CAPTURED_FUNDS: 'store_credit_captured_funds',
};

const derivedTypes = {
  ORDER_ADDED_SKU: 'order_added_sku',
  ORDER_REMOVED_SKU: 'order_removed_sku',
  ORDER_UPDATED_SKU: 'order_updated_sku',
};

export default types;
