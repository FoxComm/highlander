'use strict';

import * as actionTypes from '../actions/order-line-items';

const initialState = {
  isEditing: false,
  sortBy: 'sku',
  items: []
};

export function orderLineItems(state = initialState, action) {
  switch (action.type) {
    case actionTypes.ORDER_LINE_ITEMS_EDIT:
      return {
        ...state,
        isEditing: true,
        items: action.items
      };
    case actionTypes.ORDER_LINE_ITEMS_CANCEL_EDIT:
      return {
        ...state,
        isEditing: false,
        items: []
      };
    case actionTypes.ORDER_LINE_ITEM_INCREMENT:
      let incrementItems = state.items.map((item, idx) => {
        if (item.sku === action.sku) {
          item.quantity += 1;
        }
        return item;
      });

      return {
        ...state,
        isEditing: true,
        items: incrementItems
      };
    case actionTypes.ORDER_LINE_ITEM_DECREMENT:
      let decrementItems = state.items.map((item, idx) => {
        if (item.sku === action.sku) {
          item.quantity -= 1;
        }
        return item;
      });

      return {
        ...state,
        isEditing: true,
        items: decrementItems
      };
    default:
      return state;
  }
}