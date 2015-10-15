'use strict';

import Api from '../lib/api';

export const ORDER_REQUEST = 'ORDER_REQUEST';
export const ORDER_SUCCESS = 'ORDER_SUCCESS';
export const ORDER_FAILED = 'ORDER_FAILED';

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

export function requestOrder(refNum) {
  return {
    type: ORDER_REQUEST,
    refNum
  };
}

export function receiveOrder(refNum, json) {
  return {
    type: ORDER_SUCCESS,
    refNum,
    item: json
  };
}

export function failOrder(refNum, err) {
  return {
    type: ORDER_FAILED,
    refNum,
    err
  };
}

export function fetchOrder(refNum) {
  return dispatch => {
    dispatch(requestOrder(refNum));
    return Api.get(`/orders/${refNum}`)
      .then(json => dispatch(receiveOrder(refNum, json)))
      .catch(err => dispatch(failOrder(err)));
  };
}

function shouldFetchOrder(refNum, state) {
  const order = state.order;
  if (!order) {
    return true;
  } else if (order.isFetching) {
    return false;
  }
  return order.didInvalidate;
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

export function fetchOrderIfNeeded(refNum) {
  return (dispatch, getState) => {
    if (shouldFetchOrder(refNum, getState())) {
      return dispatch(fetchOrder(refNum));
    }
  };
}
