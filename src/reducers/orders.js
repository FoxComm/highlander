'use strict';

import { combineReducers } from 'redux';

const initialState = {
  isFetching: false,
  didInvalidate: true,
  items: []
};

export function orders(state = initialState, action) {
  switch (action.type) {
    case 'ORDERS_REQUEST':
      return {
        ...state,
        isFetching: true,
        didInvalidate: false
      };
    case 'ORDERS_SUCCESS':
      return {
        ...state,
        isFetching: false,
        didInvalidate: false,
        items: action.items
      };
    case 'ORDERS_FAILED':
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    default:
      return state;
  }
}