'use strict';

import { combineReducers } from 'redux';

const initialState = {
  isFetching: false,
  didInvalidate: false,
  items: []
};

export function orders(state = initialState, action) {
  switch (action.type) {
    case 'ORDERS_REQUEST':
      return Object.assign({}, state, {
        isFetching: true,
        didInvalidate: false
      });
    case 'ORDERS_SUCCESS':
      return Object.assign({}, state, {
        isFetching: false,
        didInvalidate: false,
        items: action.items
      });
    case 'ORDERS_FAILED':
      return Object.assign({}, state, {
        isFetching: false,
        didInvalidate: false
      });
    default:
      return state;
  }
}