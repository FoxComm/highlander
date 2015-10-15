'use strict';

import Api from '../lib/api';

export const ORDERS_REQUEST = 'ORDERS_REQUEST';
export const ORDERS_SUCCESS = 'ORDERS_SUCCESS';
export const ORDERS_FAILED = 'ORDERS_FAILED';

export function requestOrders() {
  return {
    type: ORDERS_REQUEST
  };
}

export function receiveOrders(json) {
  return {
    type: ORDERS_SUCCESS,
    items: json
  };
}

export function failOrders(err) {
  return {
    type: ORDERS_FAILED,
    err
  };
}

export function fetchOrders() {
  return dispatch => {
    dispatch(requestOrders());
    return Api.get('/orders')
      .then(json => dispatch(receiveOrders(json)))
      .catch(err => dispatch(failOrders(err)));
  };
}

function shouldFetchOrders(state) {
  const orders = state.orders;
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

// TODO: implement it in actions
//_callShippingAddressMethod(method, refNum, body = void 0) {
//  let uri = `/orders/${refNum}/shipping-address`;
//  return Api[method](uri, body)
//    .then((res) => {
//      // update shipping address for order in store
//      this.fetchOrder(refNum);
//    });
//}
//
//setShippingAddress(refNum, addressId) {
//  this._callShippingAddressMethod('patch', refNum, {addressId});
//}
//
//removeShippingAddress(refNum) {
//  this._callShippingAddressMethod('delete', refNum);
//}
