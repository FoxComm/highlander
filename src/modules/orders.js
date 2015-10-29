'use strict';

import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

export const ordersRequest = createAction('ORDERS_REQUEST');
export const ordersSuccess = createAction('ORDERS_SUCCESS');
export const ordersFailed = createAction('ORDERS_FAILED', (err, source) => ({err, source}));

export function fetchOrders() {
  return dispatch => {
    dispatch(ordersRequest());
    return Api.get('/orders')
      .then(orders => dispatch(ordersSuccess(orders)))
      .catch(err => dispatch(ordersFailed(err, fetchOrders)));
  };
}

const initialState = {
  itFetching: false,
  items: [],
  sortColumn: ''
};

const reducer = createReducer({
  [ordersRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [ordersSuccess]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      items: payload
    };
  },
  [ordersFailed]: (state, {err, source}) => {
    console.error(err);

    if (source === fetchOrders) {
      return {
        ...state,
        isFetching: false
      };
    }

    return state;
  }
}, initialState);

export default reducer;