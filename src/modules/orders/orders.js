'use strict';

import Api from '../../lib/api';
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

function shouldFetchOrders(state) {
  const orders = state.orders.orders;
  if (!orders) {
    return true;
  } else if (orders.isFetching) {
    return false;
  }
  return orders.didInvalidate;
}

export function fetchOrdersIfNeeded() {
  return (dispatch, getState) => {
    if (shouldFetchOrders(getState())) {
      return dispatch(fetchOrders());
    }
  };
}

const initialState = {
  isFetching: false,
  didInvalidate: true,
  items: [],
  sortColumn: ''
};

const reducer = createReducer({
  [ordersRequest]: (state) => {
    return {
      ...state,
      isFetching: true,
      didInvalidate: false
    };
  },
  [ordersSuccess]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      didInvalidate: false,
      items: payload
    };
  },
  [ordersFailed]: (state, {err, source}) => {
    console.error(err);

    if (source === fetchOrders) {
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    }

    return state;
  }
}, initialState);

export default reducer;
