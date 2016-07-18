
import _ from 'lodash';
import Api from 'lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import OrderParagon from 'paragons/order';

export const orderRequest = createAction('ORDER_REQUEST');
export const cartRequest = createAction('CART_REQUEST_1');
export const orderSuccess = createAction('ORDER_SUCCESS');
export const orderFailed = createAction('ORDER_FAILED', (err, source) => [err, source]);

function baseFetchOrder(url, actionBefore) {
  return dispatch => {
    dispatch(actionBefore);
    return Api.get(url)
      .then(
        order => dispatch(orderSuccess(order)),
        err => dispatch(orderFailed(err, baseFetchOrder))
      );
  };
}

export function fetchOrder(refNum) {
  return baseFetchOrder(`/orders/${refNum}`, orderRequest(refNum));
}

export function fetchCustomerCart(customerId) {
  return baseFetchOrder(`/customers/${customerId}/cart`, cartRequest(customerId));
}

export function updateOrder(id, data) {
  return dispatch => {
    dispatch(orderRequest(id));
    Api.patch(`/orders/${id}`, data)
      .then(
        order => dispatch(orderSuccess(order)),
        err => dispatch(orderFailed(id, err, updateOrder))
      );
  };
}

const initialState = {
  isFetching: false,
  failed: null,
  currentOrder: {},
};

const reducer = createReducer({
  [orderRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [cartRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [orderSuccess]: (state, payload) => {
    const order = _.get(payload, 'result', payload);

    return {
      ...state,
      isFetching: false,
      failed: null,
      currentOrder: new OrderParagon(order),
    };
  },
  [orderFailed]: (state, [err, source]) => {
    if (source === baseFetchOrder) {
      console.error(err);

      return {
        ...state,
        failed: true,
        isFetching: false
      };
    }

    return state;
  },
}, initialState);

export default reducer;
