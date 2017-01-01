
// list of all available types
// https://github.com/FoxComm/phoenix-scala/tree/master/app/services/activity

const types = {
  /* Assignments */

  ASSIGNED: 'assigned',
  UNASSIGNED: 'unassigned',
  BULK_ASSIGNED: 'bulk_assigned',
  BULK_UNASSIGNED: 'bulk_unassigned',

  /* Users */

  USER_CREATED: 'user_created',
  USER_REGISTERED: 'user_registered',
  USER_ACTIVATED: 'user_activated',
  USER_BLACKLISTED: 'user_blacklisted',
  USER_REMOVED_FROM_BLACKLIST: 'user_removed_from_blacklist',
  USER_ENABLED: 'user_enabled',
  USER_DISABLED: 'user_disabled',
  USER_UPDATED: 'user_updated',

 /* Customers */
  CUSTOMER_CREATED: 'customer_created',
  CUSTOMER_REGISTERED: 'customer_registered',
  CUSTOMER_ACTIVATED: 'customer_activated',
  CUSTOMER_UPDATED: 'customer_updated',

  /* Customer Addresses */

  USER_ADDRESS_CREATED: 'user_address_created',
  USER_ADDRESS_UPDATED: 'user_address_updated',
  USER_ADDRESS_DELETED: 'user_address_deleted',

  /* Customer Credit Cards */

  CREDIT_CARD_ADDED: 'credit_card_added',
  CREDIT_CARD_UPDATED: 'credit_card_updated',
  CREDIT_CARD_REMOVED: 'credit_card_removed',

  /* Orders */

  ORDER_STATE_CHANGED: 'order_state_changed',
  ORDER_BULK_STATE_CHANGED: 'order_bulk_state_changed',
  ORDER_REMORSE_PERIOD_INCREASED: 'order_remorse_period_increased',

  /* Carts */

  CART_CREATED: 'cart_created',

  /* Order Line Items */

  CART_LINE_ITEMS_ADDED_GIFT_CARD: 'cart_line_items_added_gift_card',
  CART_LINE_ITEMS_UPDATED_GIFT_CARD: 'cart_line_items_updated_gift_card',
  CART_LINE_ITEMS_DELETED_GIFT_CARD: 'cart_line_items_deleted_gift_card',
  CART_LINE_ITEMS_UPDATED_QUANTITIES: 'cart_line_items_updated_quantities',

  /* Order Shipping Methods */

  CART_SHIPPING_METHOD_UPDATED: 'cart_shipping_method_updated',
  CART_SHIPPING_METHOD_REMOVED: 'cart_shipping_method_removed',

  /* Order Shipping Addresses */

  CART_SHIPPING_ADDRESS_ADDED: 'cart_shipping_address_added',
  CART_SHIPPING_ADDRESS_UPDATED: 'cart_shipping_address_updated',
  CART_SHIPPING_ADDRESS_REMOVED: 'cart_shipping_address_removed',

  /* Order Payment Methods */

  CART_PAYMENT_METHOD_ADDED_CREDIT_CARD: 'cart_payment_method_added_credit_card',
  CART_PAYMENT_METHOD_ADDED_GIFT_CARD: 'cart_payment_method_added_gift_card',
  CART_PAYMENT_METHOD_ADDED_STORE_CREDIT: 'cart_payment_method_added_store_credit',
  CART_PAYMENT_METHOD_DELETED: 'cart_payment_method_deleted',
  CART_PAYMENT_METHOD_DELETED_GIFT_CARD: 'cart_payment_method_deleted_gift_card',

  /* Notes */

  NOTE_CREATED: 'note_created',
  NOTE_UPDATED: 'note_updated',
  NOTE_DELETED: 'note_deleted',

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

  /* Products */

  FULL_PRODUCT_CREATED: 'full_product_created',
  FULL_PRODUCT_UPDATED: 'full_product_updated',

  /* SKUs */
  FULL_SKU_CREATED: 'full_sku_created',
  FULL_SKU_UPDATED: 'full_sku_updated',

  /* Coupons */

  COUPON_CREATED: 'coupon_created',
  COUPON_UPDATED: 'coupon_updated',

  /* Promotions */

  PROMOTION_CREATED: 'promotion_created',
  PROMOTION_UPDATED: 'promotion_updated',
};

export const derivedTypes = {
  CART_LINE_ITEMS_ADDED_SKU: 'order_added_sku',
  CART_LINE_ITEMS_REMOVED_SKU: 'order_removed_sku',
};

export default types;
