'use strict';

import Api from '../lib/api';

export const ORDER_LINE_ITEMS_EDIT = 'ORDER_LINE_ITEMS_EDIT';
export const ORDER_LINE_ITEMS_CANCEL_EDIT = 'ORDER_LINE_ITEMS_CANCEL_EDIT';

export const ORDER_LINE_ITEM_INCREMENT = 'ORDER_LINE_ITEM_INCREMENT';
export const ORDER_LINE_ITEM_DECREMENT = 'ORDER_LINE_ITEM_DECREMENT';
export const ORDER_LINE_ITEM_ASK_DELETE = 'ORDER_LINE_ITEM_ASK_DELETE';
export const ORDER_LINE_ITEM_CANCEL_DELETE = 'ORDER_LINE_ITEM_CANCEL_DELETE';
export const ORDER_LINE_ITEM_CONFIRM_DELETE = 'ORDER_LINE_ITEM_CONFIRM_DELETE';

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

export function orderLineItemAskDelete(sku) {
  return {
    type: ORDER_LINE_ITEM_ASK_DELETE,
    sku: sku
  };
}

export function orderLineItemCancelDelete() {
  return {
    type: ORDER_LINE_ITEM_CANCEL_DELETE
  };
}

export function orderLineItemConfirmDelete() {
  return {
    type: ORDER_LINE_ITEM_CONFIRM_DELETE
  };
}