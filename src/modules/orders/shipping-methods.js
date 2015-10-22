'use strict';

import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

export const orderShippingMethodRequest = createAction('ORDER_SHIPPING_METHOD_REQUEST');
export const orderShippingMethodRequestSuccess = createAction('ORDER_SHIPPING_METHOD_REQUEST_SUCCESS');
export const orderShippingMethodRequestFailed = createAction('ORDER_SHIPPING_METHOD_REQUEST_FAILED');
export const orderShippingMethodStartEdit = createAction('ORDER_SHIPPING_METHOD_START_EDIT');
export const orderShippingMethodCancelEdit = createAction('ORDER_SHIPPING_METHOD_CANCEL_EDIT');

export function fetchShippingMethods(order) {
  return dispatch => {
    dispatch(orderShippingMethodRequest());
    return Api.get(`/shipping-methods/${order.referenceNumber}`)
      .then(methods => {
        dispatch(orderShippingMethodRequestSuccess(methods));
        dispatch(orderShippingMethodStartEdit());
      })
      .catch(err => dispatch(orderShippingMethodRequestFailed(err)));
  };
}

const initialState = {
  isEditing: false,
  isFetching: false,
  availableMethods: []
};

const reducer = createReducer({
  [orderShippingMethodRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [orderShippingMethodRequestSuccess]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      availableMethods: payload
    };
  },
  [orderShippingMethodRequestFailed]: (state, err) => {
    console.error(err);
    return {
      ...state,
      isFetching: false
    };
  },
  [orderShippingMethodStartEdit]: (state) => {
    return {
      ...state,
      isEditing: true
    };
  },
  [orderShippingMethodCancelEdit]: (state) => {
    return {
      ...state,
      isEditing: false
    };
  }
}, initialState);

export default reducer;
