'use strict';

import * as actionTypes from '../actions/orders';

const initialState = {
  isFetching: false,
  didInvalidate: true,
  item: {}
};

export function order(state = initialState, action) {
  switch (action.type) {
    case actionTypes.ORDER_REQUEST:
      return {
        ...state,
        isFetching: true,
        didInvalidate: false
      };
    case actionTypes.ORDER_SUCCESS:
      return {
        ...state,
        isFetching: false,
        didInvalidate: false,
        item: action.item
      };
    case actionTypes.ORDER_FAILED:
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    default:
      return state;
  }
}