'use strict';

import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

export const orderRequest = createAction('ORDER_REQUEST');
export const orderSuccess = createAction('ORDER_SUCCESS');
export const orderFailed = createAction('ORDER_FAILED', (err, source) => {err, source})


export function fetchOrder(refNum) {
  return dispatch => {
    dispatch(orderRequest(refNum));
    return Api.get(`/orders/${refNum}`)
      .then(order => dispatch(orderSuccess(order)))
      .catch(err => dispatch(orderFailed(err)));
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

export function fetchOrderIfNeeded(refNum) {
  return (dispatch, getState) => {
    if (shouldFetchOrder(refNum, getState())) {
      return dispatch(fetchOrder(refNum));
    }
  };
}

const initialState = {
  isFetching: false,
  didInvalidate: true,
  item: {}
};

const reducer = createReducer({
  [orderRequest]: (state) => {
    return {
      ...state,
      isFetching: true,
      didInvalidate: false
    };
  },
  [orderSuccess]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      didInvalidate: false,
      item: payload
    };
  },
  [orderFailed]: (state, {err, source}) => {
    console.error(err);

    if (source === fetchOrder) {
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