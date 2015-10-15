'use strict';

import * as actionTypes from '../actions/order-line-items';

const initialState = {
  isEditing: false,
  isDeleting: false,
  skuToDelete: '',
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
        items: incrementItems
      };
    case actionTypes.ORDER_LINE_ITEM_DECREMENT:
      let isDeleting = false;
      let skuToDelete = '';
      let decrementItems = state.items.map((item, idx) => {
        if (item.sku === action.sku) {
          if (item.quantity > 1) {
            item.quantity -= 1;
          } else {
            isDeleting = true;
            skuToDelete = action.sku;
          }
        }
        return item;
      });

      return {
        ...state,
        isDeleting: isDeleting,
        skuToDelete: skuToDelete,
        items: decrementItems
      };
    case actionTypes.ORDER_LINE_ITEM_ASK_DELETE:
      return {
        ...state,
        isDeleting: true,
        skuToDelete: action.sku
      };
    case actionTypes.ORDER_LINE_ITEM_CANCEL_DELETE:
      return {
        ...state,
        isDeleting: false,
        skuToDelete: ''
      };
    case actionTypes.ORDER_LINE_ITEM_CONFIRM_DELETE:
      return {
        ...state,
        items: state.items.filter(item => item.sku !== state.skuToDelete),
        isDeleting: false,
        skuToDelete: ''
      };
    default:
      return state;
  }
}