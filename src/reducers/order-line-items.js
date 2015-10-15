'use strict';

import * as actionTypes from '../actions/order-line-items';

const initialState = {
  isEditing: false,
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
    default:
      return state;
  }
}