/* @flow */

import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

export const clearOrder = createAction('ORDERS_CLEAR_ONE');

const _fetchOrders = createAsyncActions(
  'fetchOrders',
  function() {
    return this.api.orders.list();
  }
);

const _fetchOrder = createAsyncActions(
  'fetchOrder',
  function(referenceNumber) {
    return this.api.orders.get(referenceNumber);
  }
);

export const fetchOrders = _fetchOrders.perform;
export const fetchOrder = _fetchOrder.perform;

const initialState = {
  current: null,
  list: {},
};

const reducer = createReducer({
  [_fetchOrders.succeeded]: (state, response) => {
    return {
      ...state,
      list: response,
    };
  },
  [_fetchOrder.succeeded]: (state, response) => {
    return {
      ...state,
      current: response.result,
    };
  },
  [clearOrder]: state => {
    return {
      ...state,
      current: null,
    };
  },
}, initialState);

export default reducer;
