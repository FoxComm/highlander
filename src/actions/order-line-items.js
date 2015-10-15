'use strict';

import Api from '../lib/api';

export const ORDER_LINE_ITEMS_EDIT = 'ORDER_LINE_ITEMS_EDIT';
export const ORDER_LINE_ITEMS_CANCEL_EDIT = 'ORDER_LINE_ITEMS_CANCEL_EDIT';

export const ORDER_LINE_ITEM_INCREMENT = 'ORDER_LINE_ITEM_INCREMENT';
export const ORDER_LINE_ITEM_DECREMENT = 'ORDER_LINE_ITEM_DECREMENT';

export function orderLineItemsEdit(order) {
  return {
    type: ORDER_LINE_ITEMS_EDIT,
    items: order.lineItems.skus
  };
}

export function orderLineItemsCancelEdit() {
  return {
    type: ORDER_LINE_ITEMS_CANCEL_EDIT
  };
}

export function orderLineItemIncrement(sku) {
  return {
    type: ORDER_LINE_ITEM_INCREMENT,
    sku: sku
  };
}

export function orderLineItemDecrement(sku) {
  return {
    type: ORDER_LINE_ITEM_DECREMENT,
    sku: sku
  };
}