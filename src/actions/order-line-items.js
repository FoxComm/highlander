'use strict';

import Api from '../lib/api';

export const ORDER_LINE_ITEMS_EDIT = 'ORDER_LINE_ITEMS_EDIT';
export const ORDER_LINE_ITEMS_CANCEL_EDIT = 'ORDER_LINE_ITEMS_CANCEL_EDIT';

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